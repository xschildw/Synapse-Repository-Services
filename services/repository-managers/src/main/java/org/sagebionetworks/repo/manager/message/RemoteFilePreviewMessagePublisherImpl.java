package org.sagebionetworks.repo.manager.message;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.file.RemoteFilePreviewGenerationRequest;
import org.sagebionetworks.schema.adapter.JSONEntity;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.workers.util.aws.message.MessageQueueConfiguration;
import org.sagebionetworks.workers.util.aws.message.MessageQueueImpl;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.CreateTopicRequest;
import com.amazonaws.services.sns.model.CreateTopicResult;
import com.amazonaws.services.sns.model.ListTopicsRequest;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sns.model.SubscribeRequest;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.CreateQueueResult;
import com.amazonaws.services.sqs.model.GetQueueUrlResult;

public class RemoteFilePreviewMessagePublisherImpl implements
		RemoteFilePreviewMessagePublisher {

	static private Log logger = LogFactory.getLog(RepositoryMessagePublisherImpl.class);

	@Autowired
	AmazonSQSClient awsSQSClient;
	
	@Autowired
	MessageQueueImpl remoteFileGenerationReqMsgQueue;

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
	public RemoteFilePreviewMessagePublisherImpl(AmazonSQSClient awsSQSClient, MessageQueueImpl msgQueue) {
		this.awsSQSClient = awsSQSClient;
		this.remoteFileGenerationReqMsgQueue = msgQueue;
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
		return this.remoteFileGenerationReqMsgQueue.getQueueName();
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
