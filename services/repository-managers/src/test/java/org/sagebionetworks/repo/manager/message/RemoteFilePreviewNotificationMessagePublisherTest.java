package org.sagebionetworks.repo.manager.message;

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.sagebionetworks.repo.model.file.RemoteFilePreviewGenerationRequest;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.workers.util.aws.message.MessageQueueImpl;

import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.GetQueueUrlResult;

public class RemoteFilePreviewNotificationMessagePublisherTest {
	
	private RemoteFilePreviewGenerationRequest expectedReq;
	AmazonSQSClient sqsClient;
	MessageQueueImpl msgQ;
	RemoteFilePreviewNotificationMessagePublisherImpl publisher;

	@Before
	public void setUp() throws Exception {
		sqsClient = Mockito.mock(AmazonSQSClient.class);
		msgQ = Mockito.mock(MessageQueueImpl.class);
		
		when(msgQ.getQueueName()).thenReturn("queueName");
		when(sqsClient.getQueueUrl("queueName")).thenReturn(new GetQueueUrlResult().withQueueUrl("queueUrl"));

		publisher = new RemoteFilePreviewNotificationMessagePublisherImpl(sqsClient, msgQ);
		expectedReq = null;
	}

	@Test(expected=IllegalArgumentException.class)
	public void testPublishToQueueNullRequest() {
		publisher.publishToQueue(expectedReq);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testPublishToQueueNullSource() {
		expectedReq = new RemoteFilePreviewGenerationRequest();
		publisher.publishToQueue(expectedReq);
	}
	
	@Test
	public void testPublishToTopicDisabled() {
		publisher.setPublishToQueueEnabled(false);
		expectedReq = new RemoteFilePreviewGenerationRequest();
		expectedReq.setSource(new S3FileHandle());
		expectedReq.setDestination(new S3FileHandle());
		publisher.publishToQueue(expectedReq);
		verify(sqsClient, never()).sendMessage(anyString(), anyString());
	}
	
	@Test
	public void testPublishToTopicEnabled() throws JSONObjectAdapterException {
		publisher.setPublishToQueueEnabled(true);
		expectedReq = new RemoteFilePreviewGenerationRequest();
		S3FileHandle expectedSrc = new S3FileHandle();
		expectedSrc.setBucketName("srcBucketName");
		expectedReq.setSource(expectedSrc);
		S3FileHandle expectedDest = new S3FileHandle();
		expectedDest.setBucketName("destBucketName");
		expectedReq.setDestination(expectedDest);
		ArgumentCaptor<String> captor1 = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> captor2 = ArgumentCaptor.forClass(String.class);
		publisher.publishToQueue(expectedReq);
		verify(sqsClient).sendMessage(captor1.capture(), captor2.capture());
		String qUrl = captor1.getValue();
		String msg = captor2.getValue();
		assertEquals("queueUrl", qUrl);
		RemoteFilePreviewGenerationRequest req = EntityFactory.createEntityFromJSONString(msg, RemoteFilePreviewGenerationRequest.class);
		assertEquals(expectedReq, req);
	}

}
