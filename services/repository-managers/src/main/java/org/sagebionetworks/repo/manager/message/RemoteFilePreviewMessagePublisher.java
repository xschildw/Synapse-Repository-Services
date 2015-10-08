package org.sagebionetworks.repo.manager.message;

import org.sagebionetworks.repo.model.file.RemoteFilePreviewGenerationRequest;

import com.amazonaws.services.sqs.AmazonSQSClient;

public interface RemoteFilePreviewMessagePublisher {
	/**
	 * The name of the queue to which the messages are pushed
	 */
	public String getQueueName();
	
	/**
	 * used by tests to inject a mock client.
	 * @param awsSQSClient
	 */
	public void setAwsSQSClient(AmazonSQSClient awsSQSClient);
	
	/**
	 * Publish a file preview generation request to its topic.
	 * 
	 * @param req
	 * @return 
	 */
	public void publishToQueue(RemoteFilePreviewGenerationRequest req);

}
