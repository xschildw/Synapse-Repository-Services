package org.sagebionetworks.repo.manager.message;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.file.RemoteFilePreviewGenerationRequest;
import org.sagebionetworks.schema.adapter.JSONEntity;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.CreateTopicRequest;
import com.amazonaws.services.sns.model.CreateTopicResult;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sns.model.SubscribeRequest;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.CreateQueueResult;

public class RemoteFilePreviewMessagePublisherImpl implements
		RemoteFilePreviewMessagePublisher {

	static private Log logger = LogFactory.getLog(RepositoryMessagePublisherImpl.class);

	@Autowired
	AmazonSNSClient awsSNSClient;
	
	@Autowired
	AmazonSQSClient awsSQSClient;

	private boolean publishToTopicEnabled;
	private String queueName;
	private String topicName;
	private String topicArn;
	private TopicInfo topicInfo;

	/* Injected */
	public void setPublishToTopicEnabled(boolean f) {
		this.publishToTopicEnabled = f;
	}

	/**
	 * @param awsSNSClient
	 * @param topicName
	 * @param topicArn
	 */
	public RemoteFilePreviewMessagePublisherImpl(AmazonSQSClient awsSQSClient, String queueName, AmazonSNSClient awsSNSClient, String topicName) {
		this.awsSQSClient = awsSQSClient;
		this.awsSNSClient = awsSNSClient;
		this.topicName = topicName;
		this.queueName = queueName;
	}
	
	/**
	 * @param topicName
	 * @param topicArn
	 */
	public RemoteFilePreviewMessagePublisherImpl(String topicName) {
		this.topicName = topicName;
	}
	
	/**
	 * Used by tests to inject a mock client.
	 * @param awsSNSClient
	 */
	public void setAwsSNSClient(AmazonSNSClient client) {
		this.awsSNSClient = client;
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
		return this.queueName;
	}
	
	@Override
	public String getTopicName() {
		return this.getTopicInfoLazy().getName();
	}

	@Override
	public String getTopicArn() {
		return this.getTopicInfoLazy().getArn();
	}

	@Override
	public void publishToTopic(RemoteFilePreviewGenerationRequest req) {
		if (req == null) {
			throw new IllegalArgumentException("Request cannot be null.");
		}
		if ((req.getSource() == null) || (req.getDestination() == null)) {
			throw new IllegalArgumentException("Source and Destination cannot be null.");
		}
		if (this.publishToTopicEnabled) {
			this.publish(req, this.getTopicArn());
		}
	}
	
	private void publish(JSONEntity message, String topicArn) {
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
		awsSNSClient.publish(new PublishRequest(topicArn, json));
	}
	
	/**
	 * Get the topic info for a given type (lazy loaded).
	 * 
	 * @return
	 */
	private TopicInfo getTopicInfoLazy(){
		TopicInfo info = this.topicInfo;
		if (info == null) {
			// Create the topic
			String name = this.topicName;
			String qName = this.queueName;
			CreateTopicResult result = awsSNSClient.createTopic(new CreateTopicRequest(name));
			String arn = result.getTopicArn();
			CreateQueueResult qRes = awsSQSClient.createQueue(qName);
			String qUrl = qRes.getQueueUrl();
			SubscribeRequest sReq = new SubscribeRequest().withTopicArn(arn).withEndpoint(qUrl).withProtocol("sqs");
			awsSNSClient.subscribe(sReq);
			info = new TopicInfo(name, arn, qName);
			this.topicInfo = info;
		}
		return info;
	}

	/**
	 * Information about a topic.
	 *
	 */
	private static class TopicInfo{
		private String name;
		private String arn;
		private String queueName;
		
		public TopicInfo(String name, String arn, String qName) {
			super();
			this.name = name;
			this.arn = arn;
			this.queueName = qName;
		}
		public String getName() {
			return name;
		}
		public String getArn() {
			return arn;
		}
		public String getQueueName() {
			return queueName;
		}
	}

}
