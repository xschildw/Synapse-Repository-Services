package org.sagebionetworks.repo.manager.message;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.file.RemoteFilePreviewGenerationRequest;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.workers.util.aws.message.MessageQueueImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.CreateTopicRequest;
import com.amazonaws.services.sns.model.CreateTopicResult;
import com.amazonaws.services.sns.model.ListSubscriptionsByTopicRequest;
import com.amazonaws.services.sns.model.ListSubscriptionsByTopicResult;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sns.model.Subscription;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.CreateQueueResult;
import com.amazonaws.services.sqs.model.GetQueueAttributesRequest;
import com.amazonaws.services.sqs.model.GetQueueAttributesResult;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context-schedulers.xml" })
public class RemoteFilePreviewMessagePublisherImplAutowiredTest {
	
	@Autowired
	private RemoteFilePreviewMessagePublisherImpl remoteFilePreviewMsgPublisher;
	
	@Autowired
	private StackConfiguration stackConfig;
	
	AmazonSNSClient mockSNSClient;
	AmazonSQSClient mockSQSClient;

	@Test
	public void setUp() throws Exception {
		assertNotNull(remoteFilePreviewMsgPublisher);
		assertEquals(stackConfig.getRemoteFilePreviewGeneratorQueueName(), remoteFilePreviewMsgPublisher.getQueueName());
//		assertEquals(stackConfig.getRemoteFilePreviewMessageTopicName(), remoteFilePreviewMsgPublisher.getTopicName());
	}

	private void setupMocks(String qName, String qUrl, String qArn) {
		CreateQueueRequest expectedQueueReq = new CreateQueueRequest().withQueueName(qName);
		Map<String, String> expectedQueueAttribs = new HashMap<>();
		expectedQueueAttribs.put(MessageQueueImpl.VISIBILITY_TIMEOUT_KEY, "120");
		GetQueueAttributesResult expectedQueueAttrRes1 = new GetQueueAttributesResult().withAttributes(expectedQueueAttribs);
		List<String> attrs = Arrays.asList(MessageQueueImpl.VISIBILITY_TIMEOUT_KEY);
		when(mockSQSClient.createQueue(expectedQueueReq)).thenReturn(new CreateQueueResult().withQueueUrl(qUrl));
		when(mockSQSClient.getQueueAttributes(qUrl, attrs)).thenReturn(expectedQueueAttrRes1);
		GetQueueAttributesRequest attrsReq = new GetQueueAttributesRequest().withQueueUrl(qUrl).withAttributeNames(MessageQueueImpl.QUEUE_ARN_KEY);
		Map<String, String> expectedQueueAttribs2 = new HashMap<>();
		expectedQueueAttribs2.put(MessageQueueImpl.QUEUE_ARN_KEY, qArn);
		GetQueueAttributesResult expectedQueueAttrRes2 = new GetQueueAttributesResult().withAttributes(expectedQueueAttribs2);
		when(mockSQSClient.getQueueAttributes(attrsReq)).thenReturn(expectedQueueAttrRes2);
	}


	@Ignore
	@Test
	public void testGetArn() {
		String arn = remoteFilePreviewMsgPublisher.getTopicArn();
		assertNotNull(arn);
		assertEquals("topicArn", arn);
	}
	
	@Ignore
	@Test
	public void testPublishToTopicDisabled() {
		remoteFilePreviewMsgPublisher.setPublishToTopicEnabled(false);
		RemoteFilePreviewGenerationRequest expectedReq = new RemoteFilePreviewGenerationRequest();
		expectedReq.setSource(new S3FileHandle());
		expectedReq.setDestination(new S3FileHandle());
		remoteFilePreviewMsgPublisher.publishToTopic(expectedReq);
		verify(mockSNSClient, never()).publish(any(PublishRequest.class));
	}
	
	@Ignore
	@Test
	public void testPublishTopTopic() throws JSONObjectAdapterException {
		remoteFilePreviewMsgPublisher.setPublishToTopicEnabled(true);
		RemoteFilePreviewGenerationRequest expectedReq = new RemoteFilePreviewGenerationRequest();
		S3FileHandle expectedSrc = new S3FileHandle();
		expectedSrc.setBucketName("srcBucketName");
		expectedReq.setSource(expectedSrc);
		S3FileHandle expectedDest = new S3FileHandle();
		expectedDest.setBucketName("destBucketName");
		expectedReq.setDestination(expectedDest);
		ArgumentCaptor<PublishRequest> captor1 = ArgumentCaptor.forClass(PublishRequest.class);
		remoteFilePreviewMsgPublisher.publishToTopic(expectedReq);
		verify(mockSNSClient).publish(captor1.capture());
		assertEquals("topicArn", captor1.getValue().getTopicArn());
		String s = captor1.getValue().getMessage();
		RemoteFilePreviewGenerationRequest req = EntityFactory.createEntityFromJSONString(s, RemoteFilePreviewGenerationRequest.class);
		assertEquals(expectedReq, req);
	}

}
