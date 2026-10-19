package org.sagebionetworks.asynchronous.workers.concurrent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.database.semaphore.CountingSemaphore;
import org.sagebionetworks.repo.model.StackStatusDao;
import org.sagebionetworks.util.progress.ProgressCallback;
import org.sagebionetworks.util.progress.ProgressListener;
import org.sagebionetworks.workers.util.aws.message.MessageDrivenRunner;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.sagebionetworks.workers.util.aws.message.SqsMessageBridge;

import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlResponse;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

@ExtendWith(MockitoExtension.class)
public class ConcurrentManagerImplTest {

	@Mock
	private CountingSemaphore mockCountingSemaphore;
	@Mock
	private SqsClient mockSqsClient;
	@Mock
	private StackStatusDao mockStackStatusDao;
	@Mock
	private ProgressCallback mockCallback;
	@Mock
	private Runnable mockRunner;
	@Mock
	private MessageDrivenRunner mockWorker;
	@Mock
	private ProgressListener mockProgressListener;

	@Spy
	@InjectMocks
	ConcurrentManagerImpl manager;

	private String lockKey;
	private int lockTimeoutSec;
	private int maxLockCount;
	private String queueUrl;
	private int maxThreadCount;
	private boolean isFifoQueue;
	private String receiptHandle;
	private Message message;

	@BeforeEach
	public void before() {
		lockKey = "someLockKey";
		lockTimeoutSec = 30;
		maxLockCount = 3;
		queueUrl = "https://aws-some-queue";
		maxThreadCount = 5;
		isFifoQueue = false;
		receiptHandle = "receiptHandle";
		message = Message.builder().messageId("messageId").receiptHandle(receiptHandle).body("body").build();
	}

	@Test
	public void testIsStackAvailableForWriteWithTrue() {
		when(mockStackStatusDao.isStackReadWrite()).thenReturn(true);
		// call under test
		assertTrue(manager.isStackAvailableForWrite());
		verify(mockStackStatusDao).isStackReadWrite();
	}

	@Test
	public void testIsStackAvailableForWriteWithFalse() {
		when(mockStackStatusDao.isStackReadWrite()).thenReturn(false);
		// call under test
		assertFalse(manager.isStackAvailableForWrite());
		verify(mockStackStatusDao).isStackReadWrite();
	}

	@Test
	public void testGetCurrentTimeMS() {
		long now = System.currentTimeMillis();
		// call under test
		assertTrue(manager.getCurrentTimeMS() >= now);
	}

	@Test
	public void testSleep() throws InterruptedException {
		long start = System.currentTimeMillis();
		// call under test
		manager.sleep(100L);
		long end = System.currentTimeMillis();
		assertTrue((end - start) >= 100);
	}

	@Test
	public void testGetSqsQueueUrl() {
		String queueName = "some-queue";
		String queueUrl = "https://aws-" + queueName;
		when(mockSqsClient.getQueueUrl(any(GetQueueUrlRequest.class)))
				.thenReturn(GetQueueUrlResponse.builder().queueUrl(queueUrl).build());
		// call under test
		String resultUrl = manager.getSqsQueueUrl(queueName);
		assertEquals(queueUrl, resultUrl);
		verify(mockSqsClient).getQueueUrl(GetQueueUrlRequest.builder().queueName(queueName).build());
	}

	@Test
	public void testGetSqsQueueUrlWithNullName() {
		String queueName = null;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.getSqsQueueUrl(queueName);
		}).getMessage();
		assertEquals("queueName is required.", message);
	}

	@Test
	public void testRunWithSemaphoreLock() {
		String token = "a-token";
		when(mockCountingSemaphore.attemptToAcquireLock(any(), anyLong(), anyInt(), any())).thenReturn(Optional.of(token));
		doAnswer((InvocationOnMock i) -> {
			((ProgressListener) i.getArgument(0)).progressMade();
			return null;
		}).when(mockCallback).addProgressListener(any());

		// call under test
		manager.runWithSemaphoreLock(lockKey, lockTimeoutSec, maxLockCount, mockCallback, mockRunner);

		verify(mockCountingSemaphore).attemptToAcquireLock(lockKey, lockTimeoutSec, maxLockCount, ConcurrentManagerImpl.class.getName());
		verify(mockCallback).addProgressListener(any());
		verify(mockCountingSemaphore).refreshLockTimeout(lockKey, token, lockTimeoutSec);
		verify(mockRunner).run();
		verify(mockCountingSemaphore).releaseLock(lockKey, token);
	}
	
	@Test
	public void testRunWithSemaphoreLockWithEmptyLock() {
		when(mockCountingSemaphore.attemptToAcquireLock(any(), anyLong(), anyInt(), any())).thenReturn(Optional.empty());
		// call under test
		manager.runWithSemaphoreLock(lockKey, lockTimeoutSec, maxLockCount, mockCallback, mockRunner);

		verify(mockCountingSemaphore).attemptToAcquireLock(lockKey, lockTimeoutSec, maxLockCount, ConcurrentManagerImpl.class.getName());
		verifyNoMoreInteractions(mockCountingSemaphore);
	}

	@Test
	public void testRunWithSemaphoreLockWithRunException() {
		String token = "a-token";
		when(mockCountingSemaphore.attemptToAcquireLock(any(), anyLong(), anyInt(), any())).thenReturn(Optional.of(token));
		doAnswer((InvocationOnMock i) -> {
			((ProgressListener) i.getArgument(0)).progressMade();
			return null;
		}).when(mockCallback).addProgressListener(any());
		doThrow(new IllegalArgumentException("nope")).when(mockRunner).run();

		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.runWithSemaphoreLock(lockKey, lockTimeoutSec, maxLockCount, mockCallback, mockRunner);
		});

		verify(mockCountingSemaphore).attemptToAcquireLock(lockKey, lockTimeoutSec, maxLockCount, ConcurrentManagerImpl.class.getName());
		verify(mockCountingSemaphore).refreshLockTimeout(lockKey, token, lockTimeoutSec);
		verify(mockRunner).run();
		verify(mockCountingSemaphore).releaseLock(lockKey, token);
	}

	@Test
	public void testRunWithSemaphoreLockWitnNullLockKey() {
		lockKey = null;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.runWithSemaphoreLock(lockKey, lockTimeoutSec, maxLockCount, mockCallback, mockRunner);
		}).getMessage();
		assertEquals("lockKey is required.", message);
	}

	@Test
	public void testRunWithSemaphoreLockWitnNullCallback() {
		mockCallback = null;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.runWithSemaphoreLock(lockKey, lockTimeoutSec, maxLockCount, mockCallback, mockRunner);
		}).getMessage();
		assertEquals("callback is required.", message);
	}

	@Test
	public void testRunWithSemaphoreLockWitnNullRunner() {
		mockRunner = null;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.runWithSemaphoreLock(lockKey, lockTimeoutSec, maxLockCount, mockCallback, mockRunner);
		}).getMessage();
		assertEquals("runner is required.", message);
	}

	@Test
	public void testStartWorkerJobWithProgressMade() throws RecoverableMessageException, Exception {
		
		doAnswer((a)->{
			// We sleep to get a chance to call progressMade() before the job terminates.
			Thread.sleep(100);
			return null;
		}).when(mockWorker).run(any(ProgressCallback.class), any(com.amazonaws.services.sqs.model.Message.class));

		// call under test
		WorkerJob job = manager.startWorkerJob(queueUrl, lockTimeoutSec, mockWorker, message);
		assertNotNull(job);
		assertNotNull(job.getListener());
		// progress made should refresh the lock
		job.getListener().progressMade();
		
		waitForFuture(job.getFuture());
		
		// the listener should be removed after the job is finished so this should be a no-op.
		job.getListener().progressMade();

		verify(mockSqsClient, times(1)).changeMessageVisibility(any(ChangeMessageVisibilityRequest.class));
		verify(mockSqsClient, times(1)).changeMessageVisibility(ChangeMessageVisibilityRequest.builder()
				.queueUrl(queueUrl).receiptHandle(receiptHandle).visibilityTimeout(lockTimeoutSec).build());

		verify(mockWorker).run((ProgressCallback) job.getListener(), SqsMessageBridge.toV1(message));
		verify(mockSqsClient)
				.deleteMessage(DeleteMessageRequest.builder().queueUrl(queueUrl).receiptHandle(receiptHandle).build());
	}

	@Test
	public void testStartWorkerJobWithRecoverableException() throws RecoverableMessageException, Exception {

		doAnswer((a)->{
			// We sleep to get a chance to call progressMade() before the job terminates.
			Thread.sleep(100);
			throw new RecoverableMessageException("Try again later");
		}).when(mockWorker).run(any(ProgressCallback.class), any(com.amazonaws.services.sqs.model.Message.class));

		// call under test
		WorkerJob job = manager.startWorkerJob(queueUrl, lockTimeoutSec, mockWorker, message);
		assertNotNull(job);
		assertNotNull(job.getListener());
		// progress made should refresh the lock
		job.getListener().progressMade();
		waitForFuture(job.getFuture());

		verify(mockSqsClient, times(2)).changeMessageVisibility(any(ChangeMessageVisibilityRequest.class));
		verify(mockSqsClient, times(1)).changeMessageVisibility(ChangeMessageVisibilityRequest.builder()
				.queueUrl(queueUrl).receiptHandle(receiptHandle).visibilityTimeout(lockTimeoutSec).build());
		// second call to put the message back in the queue in 5 seconds.
		verify(mockSqsClient, times(1)).changeMessageVisibility(ChangeMessageVisibilityRequest.builder()
				.queueUrl(queueUrl).receiptHandle(receiptHandle).visibilityTimeout(2).build());

		assertNotNull(job.getFuture());
		assertTrue(job.getFuture().isDone());
		// should not throw anything.
		job.getFuture().get();

		verify(mockWorker).run((ProgressCallback) job.getListener(), SqsMessageBridge.toV1(message));
		verify(mockSqsClient, never()).deleteMessage(any(DeleteMessageRequest.class));
	}

	/**
	 * Helper to wait for a future to finish.
	 * 
	 * @param future
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	void waitForFuture(Future<Void> future) throws InterruptedException, ExecutionException {
		assertNotNull(future);
		while (!future.isDone()) {
			Thread.sleep(100);
		}
		// will throw an exception if the worker throws an exception.
		future.get();
	}

	@Test
	public void testStartWorkerJobWithOtherException() throws RecoverableMessageException, Exception {

		IllegalArgumentException toThrow = new IllegalArgumentException("Nope.");
		doThrow(toThrow).when(mockWorker).run(any(ProgressCallback.class), any(com.amazonaws.services.sqs.model.Message.class));

		// call under test
		WorkerJob job = manager.startWorkerJob(queueUrl, lockTimeoutSec, mockWorker, message);
		assertNotNull(job);
		assertNotNull(job.getListener());
		Throwable cause = assertThrows(ExecutionException.class, () -> {
			waitForFuture(job.getFuture());
		}).getCause();
		assertEquals(toThrow, cause);

		verify(mockSqsClient, never()).changeMessageVisibility(any(ChangeMessageVisibilityRequest.class));
		verify(mockSqsClient)
				.deleteMessage(DeleteMessageRequest.builder().queueUrl(queueUrl).receiptHandle(receiptHandle).build());
	}

	@Test
	public void testPollForMessagesAndStartJobsWithNoMessages() {
		when(mockSqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
				.thenReturn(ReceiveMessageResponse.builder().messages(Collections.emptyList()).build());

		// call under test
		List<WorkerJob> jobs = manager.pollForMessagesAndStartJobs(queueUrl, maxThreadCount, lockTimeoutSec,
				mockWorker, isFifoQueue);
		assertEquals(Collections.emptyList(), jobs);

		verify(mockSqsClient).receiveMessage(ReceiveMessageRequest.builder().queueUrl(queueUrl)
				.waitTimeSeconds(0).maxNumberOfMessages(maxThreadCount).visibilityTimeout(lockTimeoutSec).build());

		verify(manager, never()).startWorkerJob(any(), anyInt(), any(), any());
	}
	
	@Test
	public void testPollForMessagesAndStartJobsWithMessageAttributes() {
		when(mockSqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
				.thenReturn(ReceiveMessageResponse.builder().messages(Collections.emptyList()).build());

		when(mockWorker.getMessageAttributeNames()).thenReturn(List.of("All"));
		
		// call under test
		List<WorkerJob> jobs = manager.pollForMessagesAndStartJobs(queueUrl, maxThreadCount, lockTimeoutSec,
				mockWorker, isFifoQueue);
		assertEquals(Collections.emptyList(), jobs);

		verify(mockSqsClient).receiveMessage(ReceiveMessageRequest.builder()
			.queueUrl(queueUrl)
			.waitTimeSeconds(0)
			.maxNumberOfMessages(maxThreadCount)
			.visibilityTimeout(lockTimeoutSec)
			.messageAttributeNames("All")
			.build()
		);

		verify(manager, never()).startWorkerJob(any(), anyInt(), any(), any());
	}

	@Test
	public void testPollForMessagesAndStartJobsWithMessages() {

		List<Message> messages = List.of(Message.builder().receiptHandle("one").build(),
				Message.builder().receiptHandle("two").build());

		when(mockSqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
				.thenReturn(ReceiveMessageResponse.builder().messages(messages).build());

		// call under test
		List<WorkerJob> jobs = manager.pollForMessagesAndStartJobs(queueUrl, maxThreadCount, lockTimeoutSec,
				mockWorker, isFifoQueue);

		assertNotNull(jobs);
		assertEquals(2, jobs.size());

		verify(mockSqsClient).receiveMessage(ReceiveMessageRequest.builder().queueUrl(queueUrl)
				.waitTimeSeconds(0).maxNumberOfMessages(maxThreadCount).visibilityTimeout(lockTimeoutSec).build());

		verify(manager, times(2)).startWorkerJob(any(), anyInt(), any(), any());
		verify(manager).startWorkerJob(queueUrl, lockTimeoutSec, mockWorker, messages.get(0));
		verify(manager).startWorkerJob(queueUrl, lockTimeoutSec, mockWorker, messages.get(1));
	}

	@Test
	public void testPollForMessagesAndStartFifoJobsWithNoMessages() {
		isFifoQueue = true;
		when(mockSqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
				.thenReturn(ReceiveMessageResponse.builder().messages(Collections.emptyList()).build());

		// call under test
		List<WorkerJob> jobs = manager.pollForMessagesAndStartJobs(queueUrl, maxThreadCount, lockTimeoutSec,
				mockWorker, isFifoQueue);
		assertEquals(Collections.emptyList(), jobs);

		// Should make a single call with maxNumberOfMessages=1, then stop (empty result)
		verify(mockSqsClient).receiveMessage(ReceiveMessageRequest.builder().queueUrl(queueUrl)
				.waitTimeSeconds(0).maxNumberOfMessages(1).visibilityTimeout(lockTimeoutSec).build());

		verify(manager, never()).startWorkerJob(any(), anyInt(), any(), any());
	}

	@Test
	public void testPollForMessagesAndStartFifoJobsWithPartialMessages() {
		isFifoQueue = true;
		Message messageOne = Message.builder().receiptHandle("one").build();
		Message messageTwo = Message.builder().receiptHandle("two").build();

		when(mockSqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
				.thenReturn(ReceiveMessageResponse.builder().messages(messageOne).build())
				.thenReturn(ReceiveMessageResponse.builder().messages(messageTwo).build())
				.thenReturn(ReceiveMessageResponse.builder().messages(Collections.emptyList()).build());

		maxThreadCount = 4;

		// call under test
		List<WorkerJob> jobs = manager.pollForMessagesAndStartJobs(queueUrl, maxThreadCount, lockTimeoutSec,
				mockWorker, isFifoQueue);

		assertNotNull(jobs);
		assertEquals(2, jobs.size());

		ReceiveMessageRequest expectedRequest = ReceiveMessageRequest.builder().queueUrl(queueUrl)
				.waitTimeSeconds(0).maxNumberOfMessages(1).visibilityTimeout(lockTimeoutSec).build();

		// Should have made 3 individual calls: 2 returned messages, 3rd was empty so 4th never happens
		verify(mockSqsClient, times(3)).receiveMessage(expectedRequest);

		verify(manager, times(2)).startWorkerJob(any(), anyInt(), any(), any());
		verify(manager).startWorkerJob(queueUrl, lockTimeoutSec, mockWorker, messageOne);
		verify(manager).startWorkerJob(queueUrl, lockTimeoutSec, mockWorker, messageTwo);
	}

	@Test
	public void testPollForMessagesAndStartFifoJobsWithMaxMessages() {
		isFifoQueue = true;

		Message messageOne = Message.builder().receiptHandle("one").build();
		Message messageTwo = Message.builder().receiptHandle("two").build();
		Message messageThree = Message.builder().receiptHandle("three").build();

		when(mockSqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
				.thenReturn(ReceiveMessageResponse.builder().messages(messageOne).build())
				.thenReturn(ReceiveMessageResponse.builder().messages(messageTwo).build())
				.thenReturn(ReceiveMessageResponse.builder().messages(messageThree).build());

		maxThreadCount = 3;

		// call under test
		List<WorkerJob> jobs = manager.pollForMessagesAndStartJobs(queueUrl, maxThreadCount, lockTimeoutSec,
				mockWorker, isFifoQueue);

		assertNotNull(jobs);
		assertEquals(3, jobs.size());

		ReceiveMessageRequest expectedRequest = ReceiveMessageRequest.builder().queueUrl(queueUrl)
				.waitTimeSeconds(0).maxNumberOfMessages(1).visibilityTimeout(lockTimeoutSec).build();

		// Should have made exactly 3 calls (all returned messages, hit the limit)
		verify(mockSqsClient, times(3)).receiveMessage(expectedRequest);

		verify(manager, times(3)).startWorkerJob(any(), anyInt(), any(), any());
		verify(manager).startWorkerJob(queueUrl, lockTimeoutSec, mockWorker, messageOne);
		verify(manager).startWorkerJob(queueUrl, lockTimeoutSec, mockWorker, messageTwo);
		verify(manager).startWorkerJob(queueUrl, lockTimeoutSec, mockWorker, messageThree);
	}

	@Test
	public void testPollForMessagesAndStartJobsWithNullUrl() {
		queueUrl = null;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.pollForMessagesAndStartJobs(queueUrl, maxThreadCount, lockTimeoutSec, mockWorker, isFifoQueue);
		}).getMessage();
		assertEquals("queueUrl is required.", message);
	}

	@Test
	public void testPollForMessagesAndStartJobsWithNullWorker() {
		mockWorker = null;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.pollForMessagesAndStartJobs(queueUrl, maxThreadCount, lockTimeoutSec, mockWorker, isFifoQueue);
		}).getMessage();
		assertEquals("worker is required.", message);
	}

	@Test
	public void testPollForMessagesAndStartJobsWithCountLessThanOne() {
		maxThreadCount = 0;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.pollForMessagesAndStartJobs(queueUrl, maxThreadCount, lockTimeoutSec, mockWorker, isFifoQueue);
		}).getMessage();
		assertEquals("maxNumberOfMessages must be greater than or equals to 1.", message);
	}

	@Test
	public void testPollForMessagesAndStartJobsWithCountMoreThanTen() {
		maxThreadCount = 11;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.pollForMessagesAndStartJobs(queueUrl, maxThreadCount, lockTimeoutSec, mockWorker, isFifoQueue);
		}).getMessage();
		assertEquals("maxNumberOfMessages must be less than or equals to 10.", message);
	}

	@Test
	public void testPollForMessagesAndStartJobsWithVisibilityLessThan10() {
		lockTimeoutSec = 9;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.pollForMessagesAndStartJobs(queueUrl, maxThreadCount, lockTimeoutSec, mockWorker, isFifoQueue);
		}).getMessage();
		assertEquals("messageVisibilityTimeoutSec must be greater than or equals to 10.", message);
	}
	
	@Test
	public void testStartWorkerJobWithShutDown() throws RecoverableMessageException, Exception {
		
		doAnswer((a)->{
			// We sleep to get a chance to call progressMade() before the job terminates.
			Thread.sleep(100);
			return null;
		}).when(mockWorker).run(any(ProgressCallback.class), any(com.amazonaws.services.sqs.model.Message.class));
		
		manager.forceShutdown();

		// call under test
		WorkerJob job = manager.startWorkerJob(queueUrl, lockTimeoutSec, mockWorker, message);
		assertNotNull(job);
		assertNotNull(job.getListener());
		// progress made should refresh the lock
		job.getListener().progressMade();
		
		waitForFuture(job.getFuture());
		
		// the listener should be removed after the job is finished so this should be a no-op.
		job.getListener().progressMade();

		verify(mockSqsClient, times(1)).changeMessageVisibility(any(ChangeMessageVisibilityRequest.class));
		verify(mockSqsClient, times(1)).changeMessageVisibility(ChangeMessageVisibilityRequest.builder()
				.queueUrl(queueUrl).receiptHandle(receiptHandle).visibilityTimeout(lockTimeoutSec).build());

		verify(mockWorker).run((ProgressCallback) job.getListener(), SqsMessageBridge.toV1(message));
		// the message should not be deleted after shutdown.
		verify(mockSqsClient, never()).deleteMessage(any(DeleteMessageRequest.class));
	}
	
	@Test
	public void testStartWorkerJobWitDoubleException() throws RecoverableMessageException, Exception {
		IllegalArgumentException firstException = new IllegalArgumentException("one");

		doAnswer((a) -> {
			// We sleep to get a chance to call progressMade() before the job terminates.
			Thread.sleep(100);
			throw firstException;
		}).when(mockWorker).run(any(ProgressCallback.class), any(com.amazonaws.services.sqs.model.Message.class));

		when(mockSqsClient.deleteMessage(any(DeleteMessageRequest.class))).thenThrow(new IllegalArgumentException("two"));

		// call under test
		WorkerJob job = manager.startWorkerJob(queueUrl, lockTimeoutSec, mockWorker, message);
		assertNotNull(job);
		assertNotNull(job.getListener());
		// progress made should refresh the lock
		job.getListener().progressMade();

		ExecutionException thrown = assertThrows(ExecutionException.class, () -> {
			waitForFuture(job.getFuture());
		});
		assertEquals(firstException, thrown.getCause());

		// the listener should be removed after the job is finished so this should be a
		// no-op.
		job.getListener().progressMade();

		verify(mockSqsClient, times(1)).changeMessageVisibility(any(ChangeMessageVisibilityRequest.class));
		verify(mockSqsClient, times(1)).changeMessageVisibility(ChangeMessageVisibilityRequest.builder()
				.queueUrl(queueUrl).receiptHandle(receiptHandle).visibilityTimeout(lockTimeoutSec).build());

		verify(mockWorker).run((ProgressCallback) job.getListener(), SqsMessageBridge.toV1(message));
		verify(mockSqsClient)
				.deleteMessage(DeleteMessageRequest.builder().queueUrl(queueUrl).receiptHandle(receiptHandle).build());
	}
}
