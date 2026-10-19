package org.sagebionetworks.workers.util.aws.message;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequestEntry;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlResponse;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.QueueDoesNotExistException;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

@ExtendWith(MockitoExtension.class)
public class QueueCleanerTest {

	@Mock
	private SqsClient mockSqsClient;
	@InjectMocks
	private QueueCleaner queueCleaner;

	@Test
	public void testAttemptToEmptyQueue() {
		when(mockSqsClient.getQueueUrl(any(GetQueueUrlRequest.class))).thenReturn(GetQueueUrlResponse.builder().queueUrl("aQueueUrl").build());
		// Simulate two batches to delete
		ReceiveMessageResponse pageOne = ReceiveMessageResponse.builder().messages(
				Message.builder().messageId("id1").receiptHandle("h1").build(),
				Message.builder().messageId("id2").receiptHandle("h2").build()).build();
		ReceiveMessageResponse pageTwo = ReceiveMessageResponse.builder().messages(
				Message.builder().messageId("id3").receiptHandle("h3").build()).build();
		// page three is empty.
		ReceiveMessageResponse pageThree = ReceiveMessageResponse.builder().messages(List.of()).build();

		when(mockSqsClient.receiveMessage(any(ReceiveMessageRequest.class))).thenReturn(pageOne, pageTwo, pageThree);

		// call under test
		queueCleaner.purgeQueue("someQueue");

		verify(mockSqsClient).getQueueUrl(GetQueueUrlRequest.builder().queueName("someQueue").build());
		verify(mockSqsClient, times(3)).receiveMessage(ReceiveMessageRequest.builder()
				.maxNumberOfMessages(10)
				.waitTimeSeconds(0)
				.queueUrl("aQueueUrl")
				.build());
		// each page should be deleted as a batch.
		verify(mockSqsClient).deleteMessageBatch(DeleteMessageBatchRequest.builder()
				.queueUrl("aQueueUrl")
				.entries(
						DeleteMessageBatchRequestEntry.builder().id("id1").receiptHandle("h1").build(),
						DeleteMessageBatchRequestEntry.builder().id("id2").receiptHandle("h2").build())
				.build());
		verify(mockSqsClient).deleteMessageBatch(DeleteMessageBatchRequest.builder()
				.queueUrl("aQueueUrl")
				.entries(DeleteMessageBatchRequestEntry.builder().id("id3").receiptHandle("h3").build())
				.build());
		verify(mockSqsClient, times(2)).deleteMessageBatch(any(DeleteMessageBatchRequest.class));
	}

	@Test
	public void testQueueDoesNotExist(){
		when(mockSqsClient.getQueueUrl(any(GetQueueUrlRequest.class))).thenThrow(QueueDoesNotExistException.builder().message("Not found").build());

		// call under test, should not fail.
		queueCleaner.purgeQueue("someQueue");

		verify(mockSqsClient, never()).receiveMessage(any(ReceiveMessageRequest.class));
		verify(mockSqsClient, never()).deleteMessageBatch(any(DeleteMessageBatchRequest.class));
	}
}
