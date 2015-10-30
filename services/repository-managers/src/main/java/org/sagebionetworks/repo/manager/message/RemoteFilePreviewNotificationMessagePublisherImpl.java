package org.sagebionetworks.repo.manager.message;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sagebionetworks.repo.model.file.RemoteFilePreviewGenerationRequest;
import org.sagebionetworks.schema.adapter.JSONEntity;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.workers.util.aws.message.MessageQueueImpl;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.GetQueueUrlResult;

public class RemoteFilePreviewNotificationMessagePublisherImpl implements
		RemoteFilePreviewMessagePublisher {

	static private Log logger = LogFactory.getLog(RemoteFilePreviewNotificationMessagePublisherImpl.class);

	@Autowired
	AmazonSQSClient awsSQSClient;
	
	@Autowired
	MessageQueueImpl remoteFilePreviewNotificationMsgQueue;

	private boolean publishToQueueEnabled;
	/* Injected */
	public void setPublishToQueueEnabled(boolean f) {
		this.publishToQueueEnabled = f;
	}

	/**
	 * @param awsSNSClient
	 * @param topicName
	 * @param topicArn
	 */
	public RemoteFilePreviewNotificationMessagePublisherImpl(AmazonSQSClient awsSQSClient, MessageQueueImpl msgQueue) {
		this.awsSQSClient = awsSQSClient;
		this.remoteFilePreviewNotificationMsgQueue = msgQueue;
	}
	
	/**
	 * Used by tests to inject a mock client.
	 * @param awsSQSClient
	 */
	public void setAwsSQSClient(AmazonSQSClient client) {
		this.awsSQSClient = client;
	}

	@Override
	public String getQueueName() {
		return this.remoteFilePreviewNotificationMsgQueue.getQueueName();
	}
	
	@Override
	public void publishToQueue(RemoteFilePreviewGenerationRequest req) {
		if (req == null) {
			throw new IllegalArgumentException("Request cannot be null.");
		}
		if ((req.getSource() == null) || (req.getDestination() == null)) {
			throw new IllegalArgumentException("Source and Destination cannot be null.");
		}
		if (this.publishToQueueEnabled) {
			this.publish(req, this.getQueueName());
		}
	}
	
	private void publish(JSONEntity message, String qName) {
		GetQueueUrlResult r = awsSQSClient.getQueueUrl(qName);
		String qUrl = r.getQueueUrl();
		String json;
		try {
			json = EntityFactory.createJSONStringForEntity(message);
		} catch (JSONObjectAdapterException e) {
			// should never occur
			throw new RuntimeException(e);
		}
		if (logger.isTraceEnabled()) {
			logger.trace("Publishing a message: " + json);
		}
		// Publish the message to the topic.
		awsSQSClient.sendMessage(qUrl, json);
	}
}
