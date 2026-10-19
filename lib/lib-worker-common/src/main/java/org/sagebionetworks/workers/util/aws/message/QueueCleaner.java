package org.sagebionetworks.workers.util.aws.message;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequestEntry;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.QueueDoesNotExistException;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

/**
 * A helper to purge all messages from an AWS SQS queue.
 * 
 */
public class QueueCleaner {
	
	private static final Logger log = LogManager
			.getLogger(QueueCleaner.class);
	
	SqsClient sqsClient;

	public QueueCleaner(SqsClient sqsClient) {
		super();
		this.sqsClient = sqsClient;
	}

	
	/**
	 * Purge all messages from the queue with the given name.
	 * 
	 * Will do nothing if the queue does not exist.
	 * 
	 * @param queueName
	 */
	public void purgeQueue(String queueName){
		String messageQueueUrl = null;
		try {
			messageQueueUrl = this.sqsClient.getQueueUrl(GetQueueUrlRequest.builder().queueName(queueName).build()).queueUrl();
		} catch (QueueDoesNotExistException e) {
			log.info("Queue: "+queueName+" does not exists");
			return;
		}
		
		/*
		 * It would be nice to use {@link
		 * SqsClient#purgeQueue(software.amazon.awssdk.services.sqs.model.PurgeQueueRequest)} however, Amazon only allows it to be
		 * called every 60 seconds so it cannot be used for test that need to
		 * start with an empty queue.  Therefore, we simply pull and delete messages.
		 */
		while(true){
			ReceiveMessageRequest receiveMessageRequest = ReceiveMessageRequest.builder()
				.maxNumberOfMessages(10)
				.waitTimeSeconds(0)
				.queueUrl(messageQueueUrl)
				.build();
			ReceiveMessageResponse results = this.sqsClient.receiveMessage(receiveMessageRequest);
			deleteMessageBatch(messageQueueUrl, results.messages());
			if(results.messages().isEmpty()){
				//stop when there are no more messages.
				break;
			}
		}
	}
	
	/**
	 * Delete a batch of messages.
	 * @param batch
	 */
	private void deleteMessageBatch(String messageQueueUrl, List<Message> batch){
		if(batch != null){
			if(!batch.isEmpty()){
				List<DeleteMessageBatchRequestEntry> entryList = batch.stream()
					.map(message -> DeleteMessageBatchRequestEntry.builder()
						.id(message.messageId())
						.receiptHandle(message.receiptHandle())
						.build())
					.collect(Collectors.toList());
				sqsClient.deleteMessageBatch(DeleteMessageBatchRequest.builder()
					.queueUrl(messageQueueUrl)
					.entries(entryList)
					.build());
			}
		}
	}

}
