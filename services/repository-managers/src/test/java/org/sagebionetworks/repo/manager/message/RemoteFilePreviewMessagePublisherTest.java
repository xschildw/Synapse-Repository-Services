package org.sagebionetworks.repo.manager.message;

import static org.junit.Assert.*;

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

import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.CreateTopicRequest;
import com.amazonaws.services.sns.model.CreateTopicResult;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.CreateQueueResult;

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
		when(sqsClient.createQueue("queueName")).thenReturn(new CreateQueueResult().withQueueUrl("queueUrl"));
		publisher = new RemoteFilePreviewMessagePublisherImpl(sqsClient, "queueName", snsClient, "topicName");
		expectedReq = null;
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
