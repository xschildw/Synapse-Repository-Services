package org.sagebionetworks.repo.manager.message;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.file.RemoteFilePreviewGenerationRequest;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.CreateTopicRequest;
import com.amazonaws.services.sns.model.CreateTopicResult;
import com.amazonaws.services.sns.model.PublishRequest;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context-schedulers.xml" })
public class RemoteFilePreviewMessagePublisherImplAutowiredTest {
	
	@Autowired
	private RemoteFilePreviewMessagePublisherImpl remoteFilePreviewMsgPublisher;
	
	AmazonSNSClient mockSNSClient;

	@Before
	public void setUp() throws Exception {
		assertNotNull(remoteFilePreviewMsgPublisher);
		mockSNSClient = Mockito.mock(AmazonSNSClient.class);
		remoteFilePreviewMsgPublisher.setAwsSNSClient(mockSNSClient);
		when(mockSNSClient.createTopic(any(CreateTopicRequest.class))).thenReturn(new CreateTopicResult().withTopicArn("topicArn"));
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testGetArn() {
		String arn = remoteFilePreviewMsgPublisher.getTopicArn();
		assertNotNull(arn);
		assertEquals("topicArn", arn);
	}
	
	@Test
	public void testPublishToTopicDisabled() {
		remoteFilePreviewMsgPublisher.setPublishToTopicEnabled(false);
		RemoteFilePreviewGenerationRequest expectedReq = new RemoteFilePreviewGenerationRequest();
		expectedReq.setSource(new S3FileHandle());
		expectedReq.setDestination(new S3FileHandle());
		remoteFilePreviewMsgPublisher.publishToTopic(expectedReq);
		verify(mockSNSClient, never()).publish(any(PublishRequest.class));
	}
	
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
