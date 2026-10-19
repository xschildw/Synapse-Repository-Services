package org.sagebionetworks.workers.util.aws.message;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.util.progress.ProgressCallback;
import org.sagebionetworks.util.progress.ProgressListener;
import org.sagebionetworks.workers.util.Gate;

import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.MessageSystemAttributeName;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

@ExtendWith(MockitoExtension.class)
public class PollingMessageReceiverImplTest {

	@Mock
	private SqsClient mockSqsClient;
	@Mock
	private MessageDrivenRunner mockRunner;
	@Mock
	private ProgressCallback mockProgressCallback;
	@Mock
	private HasQueueUrl mockHasQueueUrl;

	@Mock
	private Gate mockGate;

	PollingMessageReceiverConfiguration config;
	String queueUrl;
	int messageVisibilityTimeoutSec;
	int semaphoreLockTimeoutSec;
	ReceiveMessageResponse results;
	ReceiveMessageResponse emptyResults;

	Message message;

	@BeforeEach
	public void before() {
		queueUrl = "aQueueUrl";
		messageVisibilityTimeoutSec = 60;
		semaphoreLockTimeoutSec = 60;

		// setup for a single message.
		message = Message.builder().messageId("id").receiptHandle("handle").body("body").build();
		results = ReceiveMessageResponse.builder().messages(message).build();
		emptyResults = ReceiveMessageResponse.builder().messages(List.of()).build();

		config = new PollingMessageReceiverConfiguration();
		config.setHasQueueUrl(mockHasQueueUrl);
		config.setRunner(mockRunner);
		config.setMessageVisibilityTimeoutSec(messageVisibilityTimeoutSec);
		config.setSemaphoreLockTimeoutSec(semaphoreLockTimeoutSec);
		config.setGate(mockGate);

	}

	@Test
	public void testHappyConstructor() {
		when(mockHasQueueUrl.getQueueUrl()).thenReturn(queueUrl);

		new PollingMessageReceiverImpl(mockSqsClient, config);
	}

	@Test
	public void testNullClient() {
		assertThrows(IllegalArgumentException.class, ()->{
			new PollingMessageReceiverImpl(null, config);
		});
	}

	@Test
	public void testSemaphoreLockTooSmall() {
		config.setSemaphoreLockTimeoutSec(39);
		assertThrows(IllegalArgumentException.class, ()->{
			new PollingMessageReceiverImpl(mockSqsClient, config);
		});
	}

	@Test
	public void testSemaphoreLockLessThanVisisibleTimeout() {
		config.setSemaphoreLockTimeoutSec(config
				.getMessageVisibilityTimeoutSec() - 1);
		assertThrows(IllegalArgumentException.class, ()->{
			new PollingMessageReceiverImpl(mockSqsClient, config);
		});
	}

	@Test
	public void testRunNullMessages() throws Throwable {
		when(mockHasQueueUrl.getQueueUrl()).thenReturn(queueUrl);
		PollingMessageReceiverImpl receiver = new PollingMessageReceiverImpl(
				mockSqsClient, config);

		// call under test
		receiver.run(mockProgressCallback);
		verify(mockRunner, never()).run(any(ProgressCallback.class),
				any(com.amazonaws.services.sqs.model.Message.class));
	}

	@Test
	public void testRunEmptyMessages() throws Throwable {
		when(mockHasQueueUrl.getQueueUrl()).thenReturn(queueUrl);
		when(mockSqsClient.receiveMessage(any(ReceiveMessageRequest.class))).thenReturn(emptyResults);
		PollingMessageReceiverImpl receiver = new PollingMessageReceiverImpl(
				mockSqsClient, config);

		when(mockGate.canRun()).thenReturn(true, false);

		// call under test
		receiver.run(mockProgressCallback);
		verify(mockRunner, never()).run(any(ProgressCallback.class),
				any(com.amazonaws.services.sqs.model.Message.class));
		verify(mockSqsClient, never()).deleteMessage(any(DeleteMessageRequest.class));
	}

	@Test
	public void testRunTooMessages() throws Exception {
		ReceiveMessageResponse results = ReceiveMessageResponse.builder().messages(message, message).build();
		when(mockHasQueueUrl.getQueueUrl()).thenReturn(queueUrl);
		when(mockSqsClient.receiveMessage(any(ReceiveMessageRequest.class))).thenReturn(results);

		PollingMessageReceiverImpl receiver = new PollingMessageReceiverImpl(
				mockSqsClient, config);

		when(mockGate.canRun()).thenReturn(true, false);

		assertThrows(IllegalStateException.class, ()->{
			// call under test
			receiver.run(mockProgressCallback);
		});
		verify(mockRunner, never()).run(any(ProgressCallback.class),
				any(com.amazonaws.services.sqs.model.Message.class));
		verify(mockSqsClient, never()).deleteMessage(any(DeleteMessageRequest.class));
	}

	@Test
	public void testOneMessage() throws Throwable {
		when(mockHasQueueUrl.getQueueUrl()).thenReturn(queueUrl);
		when(mockSqsClient.receiveMessage(any(ReceiveMessageRequest.class))).thenReturn(results, emptyResults);
		PollingMessageReceiverImpl receiver = new PollingMessageReceiverImpl(
				mockSqsClient, config);

		when(mockGate.canRun()).thenReturn(true, false);

		// call under test
		receiver.run(mockProgressCallback);
		// The runner receives the bridged v1 message
		verify(mockRunner, times(1)).run(mockProgressCallback, SqsMessageBridge.toV1(message));
		// The message should be deleted
		verify(mockSqsClient, times(1)).deleteMessage(DeleteMessageRequest.builder()
				.queueUrl(queueUrl)
				.receiptHandle(message.receiptHandle())
				.build());
		verify(mockProgressCallback).addProgressListener(any(ProgressListener.class));
		verify(mockProgressCallback).removeProgressListener(any(ProgressListener.class));
	}

	@Test
	public void testMessageDeleteOnException()
			throws Throwable {
		when(mockHasQueueUrl.getQueueUrl()).thenReturn(queueUrl);
		when(mockSqsClient.receiveMessage(any(ReceiveMessageRequest.class))).thenReturn(results, emptyResults);

		// setup the runner to throw an unknown exception
		doThrow(new IllegalArgumentException("Something was null")).when(
				mockRunner)
				.run(any(ProgressCallback.class), any(com.amazonaws.services.sqs.model.Message.class));

		PollingMessageReceiverImpl receiver = new PollingMessageReceiverImpl(
				mockSqsClient, config);

		when(mockGate.canRun()).thenReturn(true, false);

		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			receiver.run(mockProgressCallback);
		});
		// The message should be deleted for any non-RecoverableMessageException
		verify(mockSqsClient, times(1)).deleteMessage(DeleteMessageRequest.builder()
				.queueUrl(queueUrl)
				.receiptHandle(message.receiptHandle())
				.build());
		verify(mockSqsClient, never()).changeMessageVisibility(any(ChangeMessageVisibilityRequest.class));
		verify(mockProgressCallback).addProgressListener(any(ProgressListener.class));
		verify(mockProgressCallback).removeProgressListener(any(ProgressListener.class));
	}

	@Test
	public void testMessageNoDeleteRecoverableMessageException()
			throws Throwable {

		when(mockHasQueueUrl.getQueueUrl()).thenReturn(queueUrl);
		when(mockSqsClient.receiveMessage(any(ReceiveMessageRequest.class))).thenReturn(results, emptyResults);


		// setup the runner to throw a RecoverableMessageException
		doThrow(new RecoverableMessageException("Try again later.")).when(
				mockRunner)
				.run(any(ProgressCallback.class), any(com.amazonaws.services.sqs.model.Message.class));

		PollingMessageReceiverImpl receiver = new PollingMessageReceiverImpl(
				mockSqsClient, config);
		when(mockGate.canRun()).thenReturn(true, false);

		// call under test
		receiver.run(mockProgressCallback);
		// The message should not be deleted for RecoverableMessageException
		verify(mockSqsClient, never()).deleteMessage(any(DeleteMessageRequest.class));
		// Without a receive count the visibility falls back to the max retry timeout
		verify(mockSqsClient, times(1)).changeMessageVisibility(
				changeVisibilityRequest(PollingMessageReceiverImpl.RETRY_MAX_MESSAGE_VISIBILITY_TIMEOUT_SEC));
		verify(mockProgressCallback).addProgressListener(any(ProgressListener.class));
		verify(mockProgressCallback).removeProgressListener(any(ProgressListener.class));
	}

	@Test
	public void testMessageWithRecoverableMessageExceptionAndReceiveCount() throws Throwable {
		message = messageWithReceiveCount(3);
		when(mockHasQueueUrl.getQueueUrl()).thenReturn(queueUrl);
		when(mockSqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
			.thenReturn(ReceiveMessageResponse.builder().messages(message).build(), emptyResults);

		doThrow(new RecoverableMessageException("Try again later.")).when(mockRunner)
			.run(any(ProgressCallback.class), any(com.amazonaws.services.sqs.model.Message.class));

		PollingMessageReceiverImpl receiver = new PollingMessageReceiverImpl(mockSqsClient, config);
		when(mockGate.canRun()).thenReturn(true, false);

		// call under test
		receiver.run(mockProgressCallback);

		// The retry visibility is driven by the receive count, not the fixed max
		verify(mockSqsClient).changeMessageVisibility(changeVisibilityRequest(3));
		verify(mockSqsClient, never()).changeMessageVisibility(
				changeVisibilityRequest(PollingMessageReceiverImpl.RETRY_MAX_MESSAGE_VISIBILITY_TIMEOUT_SEC));
		verify(mockSqsClient, never()).deleteMessage(any(DeleteMessageRequest.class));
	}

	@Test
	public void testMessageWithRecoverableMessageException() throws Throwable {

		when(mockHasQueueUrl.getQueueUrl()).thenReturn(queueUrl);

		PollingMessageReceiverImpl receiver = new PollingMessageReceiverImpl(mockSqsClient, config);

		// setup the runner to throw a RecoverableMessageException
		doThrow(new RecoverableMessageException("Try again later.")).when(mockRunner).run(any(ProgressCallback.class), any(com.amazonaws.services.sqs.model.Message.class));

		int count = PollingMessageReceiverImpl.RETRY_MAX_MESSAGE_VISIBILITY_TIMEOUT_SEC + 2;

		for (int retryNumber = 1; retryNumber <= count; retryNumber++) {
			when(mockSqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
				.thenReturn(ReceiveMessageResponse.builder().messages(messageWithReceiveCount(retryNumber)).build());

			when(mockGate.canRun()).thenReturn(true, false);

			// call under test
			receiver.run(mockProgressCallback);

			int expectedTimeout = Math.min(retryNumber, PollingMessageReceiverImpl.RETRY_MAX_MESSAGE_VISIBILITY_TIMEOUT_SEC);

			// The visibility of the message should be reset
			ChangeMessageVisibilityRequest expectedRequest = changeVisibilityRequest(expectedTimeout);

			if (retryNumber <= PollingMessageReceiverImpl.RETRY_MAX_MESSAGE_VISIBILITY_TIMEOUT_SEC) {
				verify(mockSqsClient).changeMessageVisibility(expectedRequest);
			} else {
				verify(mockSqsClient, times(retryNumber - PollingMessageReceiverImpl.RETRY_MAX_MESSAGE_VISIBILITY_TIMEOUT_SEC + 1)).changeMessageVisibility(expectedRequest);
			}
		}

		verify(mockProgressCallback, times(count)).addProgressListener(any(ProgressListener.class));
		verify(mockProgressCallback, times(count)).removeProgressListener(any(ProgressListener.class));

		// The message should not be deleted for RecoverableMessageException
		verify(mockSqsClient, never()).deleteMessage(any(DeleteMessageRequest.class));
	}

	@Test
	public void testMessageWithRecoverableMessageExceptionAndRetryCountExceedMax()
			throws Throwable {

		message = messageWithReceiveCount(10);
		when(mockHasQueueUrl.getQueueUrl()).thenReturn(queueUrl);
		when(mockSqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
			.thenReturn(ReceiveMessageResponse.builder().messages(message).build(), emptyResults);

		// setup the runner to throw a RecoverableMessageException
		doThrow(new RecoverableMessageException("Try again later.")).when(
				mockRunner)
				.run(any(ProgressCallback.class), any(com.amazonaws.services.sqs.model.Message.class));

		PollingMessageReceiverImpl receiver = new PollingMessageReceiverImpl(
				mockSqsClient, config);

		when(mockGate.canRun()).thenReturn(true, false);

		// call under test
		receiver.run(mockProgressCallback);
		// The message should not be deleted for RecoverableMessageException
		verify(mockSqsClient, never()).deleteMessage(any(DeleteMessageRequest.class));
		// The visibility of the message should be reset
		verify(mockSqsClient, times(1)).changeMessageVisibility(
				changeVisibilityRequest(PollingMessageReceiverImpl.RETRY_MAX_MESSAGE_VISIBILITY_TIMEOUT_SEC));
		verify(mockProgressCallback).addProgressListener(any(ProgressListener.class));
		verify(mockProgressCallback).removeProgressListener(any(ProgressListener.class));
	}


	@Test
	public void testRunnerShouldTerminate() throws Exception {

		when(mockHasQueueUrl.getQueueUrl()).thenReturn(queueUrl);

		PollingMessageReceiverImpl receiver = new PollingMessageReceiverImpl(
				mockSqsClient, config);

		//always return a message
		when(mockSqsClient.receiveMessage(any(ReceiveMessageRequest.class))).thenReturn(results);

		//should not terminate for 3 times that we check.
		when(mockGate.canRun()).thenReturn(true, true,true,true,false);

		// call under test
		receiver.run(mockProgressCallback);

		ReceiveMessageRequest expectedRequest = ReceiveMessageRequest.builder()
			.messageSystemAttributeNames(MessageSystemAttributeName.APPROXIMATE_RECEIVE_COUNT)
			.maxNumberOfMessages(1)
			.visibilityTimeout(messageVisibilityTimeoutSec)
			.waitTimeSeconds(0)
			.queueUrl(queueUrl)
			.build();

		//since receiver.run() uses a do/while loop. we've done work for numInvocations("progressCallback.runnerShouldTerminate()") + 1 times
		verify(mockSqsClient, times(4)).receiveMessage(expectedRequest);
		verify(mockRunner, times(4)).run(any(ProgressCallback.class), any(com.amazonaws.services.sqs.model.Message.class));
		verify(mockSqsClient, times(4)).deleteMessage(any(DeleteMessageRequest.class));
		verify(mockProgressCallback, times(4)).addProgressListener(any(ProgressListener.class));
		verify(mockProgressCallback, times(4)).removeProgressListener(any(ProgressListener.class));
	}

	@Test
	public void testRunnerShouldTerminateWithMessageAttributes() throws Exception {

		when(mockHasQueueUrl.getQueueUrl()).thenReturn(queueUrl);
		when(mockSqsClient.receiveMessage(any(ReceiveMessageRequest.class))).thenReturn(results, emptyResults);
		PollingMessageReceiverImpl receiver = new PollingMessageReceiverImpl(
				mockSqsClient, config);

		when(mockGate.canRun()).thenReturn(true, false);

		when(mockRunner.getMessageAttributeNames()).thenReturn(List.of("All"));

		// call under test
		receiver.run(mockProgressCallback);

		verify(mockRunner).run(mockProgressCallback, SqsMessageBridge.toV1(message));

		verify(mockSqsClient).receiveMessage(ReceiveMessageRequest.builder()
			.messageSystemAttributeNames(MessageSystemAttributeName.APPROXIMATE_RECEIVE_COUNT)
			.messageAttributeNames("All")
			.maxNumberOfMessages(1)
			.visibilityTimeout(messageVisibilityTimeoutSec)
			.waitTimeSeconds(0)
			.queueUrl(queueUrl)
			.build()
		);
		verify(mockSqsClient).deleteMessage(DeleteMessageRequest.builder()
			.queueUrl(queueUrl)
			.receiptHandle(message.receiptHandle())
			.build()
		);
		verify(mockProgressCallback).addProgressListener(any(ProgressListener.class));
		verify(mockProgressCallback).removeProgressListener(any(ProgressListener.class));
	}

	@Test
	public void testDeleteOnShutdown() throws Throwable {
		when(mockHasQueueUrl.getQueueUrl()).thenReturn(queueUrl);
		when(mockSqsClient.receiveMessage(any(ReceiveMessageRequest.class))).thenReturn(results, emptyResults);

		PollingMessageReceiverImpl receiver = new PollingMessageReceiverImpl(
				mockSqsClient, config);

		when(mockGate.canRun()).thenReturn(true, false);

		// Simulate a JVM shutdown.
		receiver.forceShutdown();
		// call under test
		receiver.run(mockProgressCallback);
		verify(mockRunner, times(1)).run(any(ProgressCallback.class),
				any(com.amazonaws.services.sqs.model.Message.class));
		// The message should not be deleted after a shutdown.
		verify(mockSqsClient, never()).deleteMessage(any(DeleteMessageRequest.class));
		verify(mockProgressCallback).addProgressListener(any(ProgressListener.class));
		verify(mockProgressCallback).removeProgressListener(any(ProgressListener.class));
	}

	private Message messageWithReceiveCount(int receiveCount) {
		return message.toBuilder()
			.attributes(Map.of(MessageSystemAttributeName.APPROXIMATE_RECEIVE_COUNT, String.valueOf(receiveCount)))
			.build();
	}

	private ChangeMessageVisibilityRequest changeVisibilityRequest(int visibilityTimeoutSec) {
		return ChangeMessageVisibilityRequest.builder()
			.queueUrl(queueUrl)
			.receiptHandle(message.receiptHandle())
			.visibilityTimeout(visibilityTimeoutSec)
			.build();
	}

}
