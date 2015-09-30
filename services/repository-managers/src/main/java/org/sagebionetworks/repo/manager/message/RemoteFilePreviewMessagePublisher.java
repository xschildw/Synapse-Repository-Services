package org.sagebionetworks.repo.manager.message;

import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.file.RemoteFilePreviewGenerationRequest;
import org.sagebionetworks.repo.model.message.ChangeMessage;

import com.amazonaws.services.sns.AmazonSNSClient;

public interface RemoteFilePreviewMessagePublisher {
	/**
	 * Get the name of the topic where the messages are published.
	 * 
	 * @return
	 */
	public String getTopicName();
	
	/**
	 * The ARN for the topic where messages are published.
	 * 
	 * @return
	 */
	public String getTopicArn();
	
	/**
	 * Used by tests to inject a mock client.
	 * @param awsSNSClient
	 */
	public void setAwsSNSClient(AmazonSNSClient awsSNSClient);
	
	/**
	 * Publish a file preview generation request to its topic.
	 * 
	 * @param req
	 * @return 
	 */
	public void publishToTopic(RemoteFilePreviewGenerationRequest req);

}
