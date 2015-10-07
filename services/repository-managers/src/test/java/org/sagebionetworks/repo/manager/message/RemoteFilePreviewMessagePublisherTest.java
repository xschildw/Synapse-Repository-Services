package org.sagebionetworks.repo.manager.message;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import static org.mockito.Mockito.*;
import static org.mockito.Matchers.*;

import org.mockito.ArgumentCaptor;
import org.sagebionetworks.repo.model.file.RemoteFilePreviewGenerationRequest;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.workers.util.aws.message.MessageQueueImpl;

import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.CreateTopicRequest;
import com.amazonaws.services.sns.model.CreateTopicResult;
import com.amazonaws.services.sns.model.ListSubscriptionsByTopicRequest;
import com.amazonaws.services.sns.model.ListSubscriptionsByTopicResult;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sns.model.SubscribeRequest;
import com.amazonaws.services.sns.model.Subscription;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.CreateQueueResult;
import com.amazonaws.services.sqs.model.GetQueueAttributesRequest;
import com.amazonaws.services.sqs.model.GetQueueAttributesResult;
import com.amazonaws.services.sqs.model.SetQueueAttributesRequest;

public class RemoteFilePreviewMessagePublisherTest {
	
	private RemoteFilePreviewGenerationRequest expectedReq;
	AmazonSNSClient snsClient;
	AmazonSQSClient sqsClient;
	RemoteFilePreviewMessagePublisherImpl publisher;

	@Before
	public void setUp() throws Exception {
		snsClient = Mockito.mock(AmazonSNSClient.class);
		sqsClient = Mockito.mock(AmazonSQSClient.class);
		when(snsClient.createTopic(any(CreateTopicRequest.class))).thenReturn(new CreateTopicResult().withTopicArn("topicArn"));

		setupMocks("queueName", "queueUrl", "queueArn");
		setupMocks("queueName-dl", "queueUrl-dl", "queueArn-dl");
		
		GetQueueAttributesRequest attrsReq = new GetQueueAttributesRequest().withQueueUrl("queueUrl").withAttributeNames(MessageQueueImpl.REDRIVE_POLICY_KEY);
		Map<String, String> expectedQueueAttribs = new HashMap<>();
		String expectedRedrivePolicy = String.format("{\"maxReceiveCount\":\"%d\", \"deadLetterTargetArn\":\"%s\"}", 10, "queueArn-dl");
		expectedQueueAttribs.put(MessageQueueImpl.REDRIVE_POLICY_KEY, expectedRedrivePolicy);
		GetQueueAttributesResult expectedQueueAttrRes = new GetQueueAttributesResult().withAttributes(expectedQueueAttribs);
		when(sqsClient.getQueueAttributes(attrsReq)).thenReturn(expectedQueueAttrRes);
		
		ListSubscriptionsByTopicRequest req = new ListSubscriptionsByTopicRequest("topicArn");
		ListSubscriptionsByTopicResult emptyRes = new ListSubscriptionsByTopicResult().withSubscriptions(new ArrayList<Subscription>());
		ListSubscriptionsByTopicResult res = new ListSubscriptionsByTopicResult().withSubscriptions(new Subscription().withProtocol(MessageQueueImpl.PROTOCOL_SQS).withEndpoint("queueArn").withTopicArn("topicArn"));
		when(snsClient.listSubscriptionsByTopic(req)).thenReturn(emptyRes, res);
		// SubscribeRequest sReq = new SubscribeRequest().withProtocol(MessageQueueImpl.PROTOCOL_SQS).withEndpoint("queueArn").withTopicArn("topicArn");
		
		attrsReq = new GetQueueAttributesRequest().withQueueUrl("queueUrl").withAttributeNames(MessageQueueImpl.POLICY_KEY);
		expectedQueueAttribs = new HashMap<String, String>();
		expectedQueueAttribs.put(MessageQueueImpl.POLICY_KEY, String.format(MessageQueueImpl.GRAN_SET_MESSAGE_TEMPLATE, "queueArn", "[\"topicArn\"]"));
		when(sqsClient.getQueueAttributes(attrsReq)).thenReturn(expectedQueueAttrRes);
			
		publisher = new RemoteFilePreviewMessagePublisherImpl(sqsClient, "queueName", snsClient, "topicName");
		expectedReq = null;
	}

	private void setupMocks(String qName, String qUrl, String qArn) {
		CreateQueueRequest expectedQueueReq = new CreateQueueRequest().withQueueName(qName);
		Map<String, String> expectedQueueAttribs = new HashMap<>();
		expectedQueueAttribs.put(MessageQueueImpl.VISIBILITY_TIMEOUT_KEY, "120");
		GetQueueAttributesResult expectedQueueAttrRes1 = new GetQueueAttributesResult().withAttributes(expectedQueueAttribs);
		List<String> attrs = Arrays.asList(MessageQueueImpl.VISIBILITY_TIMEOUT_KEY);
		when(sqsClient.createQueue(expectedQueueReq)).thenReturn(new CreateQueueResult().withQueueUrl(qUrl));
		when(sqsClient.getQueueAttributes(qUrl, attrs)).thenReturn(expectedQueueAttrRes1);
		GetQueueAttributesRequest attrsReq = new GetQueueAttributesRequest().withQueueUrl(qUrl).withAttributeNames(MessageQueueImpl.QUEUE_ARN_KEY);
		Map<String, String> expectedQueueAttribs2 = new HashMap<>();
		expectedQueueAttribs2.put(MessageQueueImpl.QUEUE_ARN_KEY, qArn);
		GetQueueAttributesResult expectedQueueAttrRes2 = new GetQueueAttributesResult().withAttributes(expectedQueueAttribs2);
		when(sqsClient.getQueueAttributes(attrsReq)).thenReturn(expectedQueueAttrRes2);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testPublishToTopicNullRequest() {
		publisher.publishToTopic(expectedReq);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testPublishToTopicNullSource() {
		expectedReq = new RemoteFilePreviewGenerationRequest();
		publisher.publishToTopic(expectedReq);
	}
	
	@Test
	public void testPublishToTopicDisabled() {
		publisher.setPublishToTopicEnabled(false);
		expectedReq = new RemoteFilePreviewGenerationRequest();
		expectedReq.setSource(new S3FileHandle());
		expectedReq.setDestination(new S3FileHandle());
		publisher.publishToTopic(expectedReq);
		verify(snsClient, never()).publish(any(PublishRequest.class));
	}
	
	@Test
	public void testPublishToTopicEnabled() throws JSONObjectAdapterException {
		publisher.setPublishToTopicEnabled(true);
		expectedReq = new RemoteFilePreviewGenerationRequest();
		S3FileHandle expectedSrc = new S3FileHandle();
		expectedSrc.setBucketName("srcBucketName");
		expectedReq.setSource(expectedSrc);
		S3FileHandle expectedDest = new S3FileHandle();
		expectedDest.setBucketName("destBucketName");
		expectedReq.setDestination(expectedDest);
		ArgumentCaptor<PublishRequest> captor1 = ArgumentCaptor.forClass(PublishRequest.class);
		publisher.publishToTopic(expectedReq);
		verify(snsClient).publish(captor1.capture());
		assertEquals("topicArn", captor1.getValue().getTopicArn());
		String s = captor1.getValue().getMessage();
		RemoteFilePreviewGenerationRequest req = EntityFactory.createEntityFromJSONString(s, RemoteFilePreviewGenerationRequest.class);
		assertEquals(expectedReq, req);
	}

}
