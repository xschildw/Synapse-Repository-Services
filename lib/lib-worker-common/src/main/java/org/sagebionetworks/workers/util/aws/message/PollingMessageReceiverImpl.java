package org.sagebionetworks.workers.util.aws.message;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.util.progress.ProgressCallback;
import org.sagebionetworks.util.progress.ProgressListener;
import org.sagebionetworks.util.progress.ProgressingRunner;
import org.sagebionetworks.workers.util.Gate;

import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.MessageSystemAttributeName;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

/**
 * A MessageReceiver that uses long polling to fetch messages from AWS SQS.
 * 
 */
public class PollingMessageReceiverImpl implements ProgressingRunner {

	static final List<MessageSystemAttributeName> INCLUDED_ATTRIBUTES = List.of(
		MessageSystemAttributeName.APPROXIMATE_RECEIVE_COUNT
	);
	
	private static final Logger log = LogManager.getLogger(PollingMessageReceiverImpl.class);

	/*
	 * The maximum amount of time in seconds that this receiver will wait for a
	 * message to appear in the queue.
	 */
	public static int MAX_MESSAGE_POLL_TIME_SEC = 2;
	
	/*
	 * Used for message that failed but should be returned to the queue.  For this case
	 * we want to be able to retry the message quickly, so it is set to a max of 5 seconds. 
	 */
	public static int RETRY_MAX_MESSAGE_VISIBILITY_TIMEOUT_SEC = 5;
	/*
	 * Since this receiver does long polling for messages we need to ensure
	 * semaphore lock timeouts are not less than poll time.
	 */
	public static int MIN_SEMAPHORE_LOCK_TIMEOUT_SEC = MAX_MESSAGE_POLL_TIME_SEC * 2;

	private final SqsClient sqsClient;
	private final String messageQueueUrl;
	private final Integer messageVisibilityTimeoutSec;
	private final MessageDrivenRunner runner;
	private final Gate gate;
	// We do not want to delete any messages when the JVM is being shut down. PLFM-6758.
	private volatile boolean isShutdown = false; 

	/**
	 * 
	 * @param sqsClient
	 *            An SqsClient configured with credentials.
	 * @param config
	 *            Configuration information for this message receiver.
	 */
	public PollingMessageReceiverImpl(SqsClient sqsClient, PollingMessageReceiverConfiguration config) {
		super();
		if (sqsClient == null) {
			throw new IllegalArgumentException("SqsClient cannot be null");
		}
		this.sqsClient = sqsClient;
		if (config == null) {
			throw new IllegalArgumentException(
					"PollingMessageReceiverConfiguration cannot be null");
		}
		if (config.getHasQueueUrl() == null) {
			throw new IllegalArgumentException(
					"PollingMessageReceiverConfiguration.hasQueueUrl cannot be null");
		}
		if (config.getHasQueueUrl().getQueueUrl() == null) {
			throw new IllegalArgumentException(
					"PollingMessageReceiverConfiguration.hasQueueUrl.queueUrl cannot be null");
		}
		if (config.getMessageVisibilityTimeoutSec() == null) {
			throw new IllegalArgumentException(
					"PollingMessageReceiverConfiguration.messageVisibilityTimeoutSec cannot be null");
		}
		if (config.getSemaphoreLockTimeoutSec() == null) {
			throw new IllegalArgumentException(
					"PollingMessageReceiverConfiguration.semaphoreLockTimeoutSec cannot be null");
		}
		if (config.getSemaphoreLockTimeoutSec() < MIN_SEMAPHORE_LOCK_TIMEOUT_SEC) {
			throw new IllegalArgumentException(
					"PollingMessageReceiverConfiguration.semaphoreLockTimeoutSec must be at least "
							+ MIN_SEMAPHORE_LOCK_TIMEOUT_SEC + " seconds.");
		}
		if (config.getSemaphoreLockTimeoutSec() < config
				.getMessageVisibilityTimeoutSec()) {
			throw new IllegalArgumentException(
					"PollingMessageReceiverConfiguration.semaphoreLockTimeoutSec cannot be less than pollingMessageReceiverConfiguration.messageVisibilityTimeoutSec ");
		}
		if (config.getRunner() == null) {
			throw new IllegalArgumentException(
					"PollingMessageReceiverConfiguration.runner cannot be null");
		}
		this.messageQueueUrl = config.getHasQueueUrl().getQueueUrl();
		this.messageVisibilityTimeoutSec = config.getMessageVisibilityTimeoutSec();
		this.gate = config.getGate();
		this.runner = config.getRunner();
		
		isShutdown = false;
		// We need to know when the JVM is shutting down.
		Runtime.getRuntime().addShutdownHook(new Thread(()->{
			isShutdown = true;
			log.warn("JVM is shutting down. No messages will be deleted.");
		}));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sagebionetworks.workers.util.progress.ProgressingRunner#run(org.
	 * sagebionetworks.workers.util.progress.ProgressCallback)
	 */
	@Override
	public void run(final ProgressCallback containerProgressCallback) throws Exception {
		Message message = null;
		while(true) {
			if (gate != null && !gate.canRun()) {
				log.trace(gate.getClass().getSimpleName() +" is closed for " + runner.getClass().getSimpleName());
				break;
			}
			message = pollForMessage();
			if(message != null){
				processMessage(containerProgressCallback, message);
			} else {
				Thread.sleep(1000);
			}
		}
		log.trace("There is no more messages for "+runner.getClass().getSimpleName());
	}
	
	/**
	 * Poll for a single message.
	 * @return
	 */
	private Message pollForMessage(){
		log.trace("Getting message for " + runner.getClass().getSimpleName());
		ReceiveMessageRequest.Builder request = ReceiveMessageRequest.builder()
			.messageSystemAttributeNames(INCLUDED_ATTRIBUTES)
			.maxNumberOfMessages(1)
			.queueUrl(this.messageQueueUrl)
			.visibilityTimeout(this.messageVisibilityTimeoutSec)
			// NOTE: it is very important that waitTimeSeconds is kept at 0. Otherwise,
			// the call will wait by holding on to a connection the connection pool,
			// thus preventing other worker threads from checking for messages until this thread receives a message.
			.waitTimeSeconds(0);
		
		if (runner.getMessageAttributeNames() != null && !runner.getMessageAttributeNames().isEmpty()) {
			request.messageAttributeNames(runner.getMessageAttributeNames());
		}
		
		// Poll for one message.
		ReceiveMessageResponse results = this.sqsClient.receiveMessage(request.build());
	
		if (results == null || !results.hasMessages() || results.messages().isEmpty()) {
			return null;
		}
		
		List<Message> messages = results.messages();
	
		if (messages.size() != 1) {
			throw new IllegalStateException("Expected only one message but received: " + messages.size());
		}
		
		final Message message = messages.get(0);
		
		if (message == null) {
			throw new IllegalStateException("Message list contains a null message");
		}
		
		return message;
	}

	/**
	 * Process a single message.
	 * @param containerProgressCallback
	 * @param message
	 * @throws Exception
	 */
	private void processMessage(final ProgressCallback containerProgressCallback, final Message message) throws Exception {
		log.trace("Processing message for "+runner.getClass().getSimpleName());
		boolean deleteMessage = true;
		// Listen to callback events
		ProgressListener listener = () -> resetMessageVisibilityTimeout(message);
		// add a listener for this message
		containerProgressCallback.addProgressListener(listener);
		try {
			// Let the runner handle the message.
			runner.run(containerProgressCallback, SqsMessageBridge.toV1(message));

		} catch (RecoverableMessageException e) {
			// this is the only case where we do not delete the message.
			deleteMessage = false;
			if (log.isDebugEnabled()) {
				log.debug("Message will be returned to the queue", e);
			}
			// Ensure this message is visible again within a short period of time
			int retryVisibility = getRetryVisibilityTimeout(message);
			resetMessageVisibilityTimeout(message, retryVisibility);
		} finally {
			// unconditionally remove the listener for this message
			containerProgressCallback.removeProgressListener(listener);
			if (deleteMessage) {
				deleteMessage(message);
			}
		}
	}
	
	private static int getRetryVisibilityTimeout(Message message) {
		if (!message.hasAttributes()) {
			return RETRY_MAX_MESSAGE_VISIBILITY_TIMEOUT_SEC;
		}
		// The ApproximateReceiveCount is an approximation of how many times the message was received, 
		// this gives us an idea of how many times it was "retried". 
		// We use this value to retry a message that wasn't processed due to a RecoverableMessageException, we need a value that is not too
		// short (e.g. if something is not ready now, chances are that is not ready right away) but at the same time not too
		// long, in the past we always used a 5 seconds delay but this leads a substantial slow down when 
		// fetching table query results or in integration tests
		String receiveCountAttr = message.attributes().get(MessageSystemAttributeName.APPROXIMATE_RECEIVE_COUNT);
		
		if (receiveCountAttr == null) {
			return RETRY_MAX_MESSAGE_VISIBILITY_TIMEOUT_SEC;
		}
		
		// We being with 1 second per retry up to a max of 5 seconds, this allows to retry quickly but not too many times too quickly 
		int retryCount = Integer.parseInt(receiveCountAttr);
		
		return Math.min(retryCount, RETRY_MAX_MESSAGE_VISIBILITY_TIMEOUT_SEC);
	}
	
	/**
	 * Force the permanent shutdown of this object.
	 */
	public void forceShutdown() {
		isShutdown = true;
	}
	

	/**
	 * Delete the given message from the queue.
	 * 
	 * @param message
	 */
	protected void deleteMessage(Message message) {
		if(isShutdown) {
			log.error(String.format("The message will not be deleted because the JVM is shutting down. QueueUrl: '%s' messageId: '%s'", this.messageQueueUrl, message.messageId()));
			return;
		}
		this.sqsClient.deleteMessage(DeleteMessageRequest.builder()
			.queueUrl(this.messageQueueUrl)
			.receiptHandle(message.receiptHandle())
			.build());
	}

	/**
	 * Reset the visibility timeout of the given message using the configured messageVisibilityTimeoutSec. Called when progress
	 * is made for a given message.
	 * 
	 * @param message
	 */
	protected void resetMessageVisibilityTimeout(Message message) {
		resetMessageVisibilityTimeout(message, this.messageVisibilityTimeoutSec);
	}
	
	/**
	 * Reset the visibility timeout of the given message to the provided using the provided visibilityTimeoutSec.
	 * @param message
	 * @param visibilityTimeoutSec
	 */
	protected void resetMessageVisibilityTimeout(Message message, int visibilityTimeoutSec) {
		this.sqsClient.changeMessageVisibility(ChangeMessageVisibilityRequest.builder()
			.queueUrl(this.messageQueueUrl)
			.receiptHandle(message.receiptHandle())
			.visibilityTimeout(visibilityTimeoutSec)
			.build());
	}

}
