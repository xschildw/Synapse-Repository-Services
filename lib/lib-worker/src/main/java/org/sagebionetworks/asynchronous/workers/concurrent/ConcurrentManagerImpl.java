package org.sagebionetworks.asynchronous.workers.concurrent;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sagebionetworks.database.semaphore.CountingSemaphore;
import org.sagebionetworks.repo.model.StackStatusDao;
import org.sagebionetworks.util.ValidateArgument;
import org.sagebionetworks.util.progress.ProgressCallback;
import org.sagebionetworks.util.progress.ProgressListener;
import org.sagebionetworks.workers.util.aws.message.MessageDrivenRunner;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.sagebionetworks.workers.util.aws.message.SqsMessageBridge;

import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

public class ConcurrentManagerImpl implements ConcurrentManager {

	private static final int TWO_SECONDS = 2;
	private static final Log log = LogFactory.getLog(ConcurrentWorkerStack.class);

	private final CountingSemaphore countingSemaphore;
	private final ExecutorService executorService;
	private final SqsClient sqsClient;
	private final StackStatusDao stackStatusDao;
	private volatile boolean isShutdown;

	public ConcurrentManagerImpl(CountingSemaphore countingSemaphore, SqsClient sqsClient,
			StackStatusDao stackStatusDao) {
		super();
		this.countingSemaphore = countingSemaphore;
		this.sqsClient = sqsClient;
		this.stackStatusDao = stackStatusDao;
		/*
		 * Note: We do not use a fix sized thread pool because we do not know how many
		 * threads will be needed for all of the workers on this machine. Each SQS
		 * message is received from a queue prior to being submitted to this thread pool
		 * for processing. Once a message is received it must be processed immediately.
		 * If the message processing is delayed due to a lack of an available thread on
		 * this machine then a live-lock scenario will be created. In such a scenario,
		 * other machines with available threads will be idle, while the held messages
		 * on this machine waits for an available threads. <p> The cached thread pool
		 * allows us guarantee that every messages received has a thread immediately
		 * available, while at it will reuse threads already allocated (until they
		 * expire).
		 */
		this.executorService = Executors.newCachedThreadPool();

		isShutdown = false;
		// We need to know when the JVM is shutting down.
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			isShutdown = true;
			log.warn("JVM is shutting down. No messages will be deleted.");
		}));
	}

	@Override
	public boolean isStackAvailableForWrite() {
		return stackStatusDao.isStackReadWrite();
	}

	@Override
	public long getCurrentTimeMS() {
		return System.currentTimeMillis();
	}

	@Override
	public void sleep(long sleepTimeMS) throws InterruptedException {
		Thread.sleep(sleepTimeMS);
	}

	@Override
	public String getSqsQueueUrl(String queueName) {
		ValidateArgument.required(queueName, "queueName");
		return sqsClient.getQueueUrl(GetQueueUrlRequest.builder().queueName(queueName).build()).queueUrl();
	}

	@Override
	public void runWithSemaphoreLock(String lockKey, int lockTimeoutSec, int maxLockCount, ProgressCallback callback,
			Runnable runner) {
		ValidateArgument.required(lockKey, "lockKey");
		ValidateArgument.required(callback, "callback");
		ValidateArgument.required(runner, "runner");

		Optional<String> optional = countingSemaphore.attemptToAcquireLock(lockKey, lockTimeoutSec, maxLockCount, ConcurrentManagerImpl.class.getName());
		optional.ifPresent((token)->{
			callback.addProgressListener(() -> {
				countingSemaphore.refreshLockTimeout(lockKey, token, lockTimeoutSec);
			});
			try {
				runner.run();
			} finally {
				countingSemaphore.releaseLock(lockKey, token);
			}
		});
	}

	@Override
	public List<WorkerJob> pollForMessagesAndStartJobs(String queueUrl, int maxNumberOfMessages,
			int messageVisibilityTimeoutSec, MessageDrivenRunner worker, boolean isFifoQueue) {
		ValidateArgument.required(queueUrl, "queueUrl");
		ValidateArgument.required(worker, "worker");
		ValidateArgument.requirement(maxNumberOfMessages >= 1,
				"maxNumberOfMessages must be greater than or equals to 1.");
		ValidateArgument.requirement(maxNumberOfMessages <= 10,
				"maxNumberOfMessages must be less than or equals to 10.");
		ValidateArgument.requirement(messageVisibilityTimeoutSec >= 10,
				"messageVisibilityTimeoutSec must be greater than or equals to 10.");

		// Requesting `n` messages for a FIFO queue may return messages with the same MessageGroupId
		// Instead, receive 1 message `n` times, which ensures in-order processing
		// SQS will not provide the next message in the group until the previous message is deleted or its visibility timeout expires.
		int requestCount = isFifoQueue ? maxNumberOfMessages : 1;
		int messagesPerRequest = isFifoQueue ? 1 : maxNumberOfMessages;

		List<WorkerJob> jobs = new ArrayList<>(maxNumberOfMessages);

		for (int i = 0; i < requestCount; i++) {
			ReceiveMessageRequest.Builder request = ReceiveMessageRequest.builder()
					.queueUrl(queueUrl)
					.waitTimeSeconds(0)
					.maxNumberOfMessages(messagesPerRequest)
					.visibilityTimeout(messageVisibilityTimeoutSec);

			if (worker.getMessageAttributeNames() != null && !worker.getMessageAttributeNames().isEmpty()) {
				request.messageAttributeNames(worker.getMessageAttributeNames());
			}

			// Poll for messages.
			List<Message> messages = sqsClient.receiveMessage(request.build()).messages();
			if (messages.isEmpty()) {
				break;
			}
			// For each message, start a new job and add it to the list of jobs to return.
			jobs.addAll(messages.stream()
					.map((message) -> startWorkerJob(queueUrl, messageVisibilityTimeoutSec, worker, message))
					.collect(Collectors.toList())
			);
		}
		return jobs;
	}

	/**
	 * For the given message, submit a new worker instance to the thread pool.
	 * 
	 * @param queueUrl
	 * @param messageVisibilityTimeoutSec
	 * @param worker
	 * @param message
	 * @return
	 */
	WorkerJob startWorkerJob(String queueUrl, int messageVisibilityTimeoutSec, MessageDrivenRunner worker,
			Message message) {
		ConcurrentProgressCallback callback = new ConcurrentProgressCallback(messageVisibilityTimeoutSec);
		ProgressListener listener = () -> {
			sqsClient.changeMessageVisibility(ChangeMessageVisibilityRequest.builder().queueUrl(queueUrl)
					.receiptHandle(message.receiptHandle()).visibilityTimeout(messageVisibilityTimeoutSec).build());
		};
		callback.addProgressListener(listener);
		Future<Void> future = executorService.submit(() -> {
			boolean deleteMessage = true;
			try {
				worker.run(callback, SqsMessageBridge.toV1(message));
			} catch (RecoverableMessageException e) {
				deleteMessage = false;
				sqsClient.changeMessageVisibility(ChangeMessageVisibilityRequest.builder().queueUrl(queueUrl)
						.receiptHandle(message.receiptHandle()).visibilityTimeout(TWO_SECONDS).build());
			} finally {
				try {
					callback.removeProgressListener(listener);
					if (deleteMessage && !isShutdown) {
						sqsClient.deleteMessage(DeleteMessageRequest.builder().queueUrl(queueUrl)
								.receiptHandle(message.receiptHandle()).build());
					}
				} catch (Exception e) {
					log.warn("failed to delete message", e);
				}
			}
			return null;
		});
		return new WorkerJob(future, callback);
	}

	public void forceShutdown() {
		isShutdown = true;
	}

}
