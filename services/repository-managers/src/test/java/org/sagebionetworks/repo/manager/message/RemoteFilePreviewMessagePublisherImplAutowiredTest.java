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
import org.junit.AfterClass;
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
import com.amazonaws.services.sqs.model.PurgeQueueRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context-schedulers.xml" })
@Ignore
public class RemoteFilePreviewMessagePublisherImplAutowiredTest {
	
	@Autowired
	private RemoteFilePreviewMessagePublisherImpl remoteFilePreviewMsgPublisher;
	
	@Autowired
	private AmazonSQSClient awsSQSClient;
	
	private String qUrl;
	
	@Before
	public void setUp() throws Exception {
		assertNotNull(remoteFilePreviewMsgPublisher);
		qUrl = awsSQSClient.getQueueUrl(remoteFilePreviewMsgPublisher.getQueueName()).getQueueUrl();
		awsSQSClient.purgeQueue(new PurgeQueueRequest().withQueueUrl(qUrl));
	}
	
	@After
	public void tearDown() throws Exception {
		awsSQSClient.purgeQueue(new PurgeQueueRequest().withQueueUrl(qUrl));
	}

	@Test
	public void testPublishToTopicDisabled() {
		remoteFilePreviewMsgPublisher.setPublishToQueueEnabled(false);
		RemoteFilePreviewGenerationRequest expectedReq = new RemoteFilePreviewGenerationRequest();
		expectedReq.setSource(new S3FileHandle());
		expectedReq.setDestination(new S3FileHandle());
		remoteFilePreviewMsgPublisher.publishToQueue(expectedReq);
		ReceiveMessageResult rMsgRes = awsSQSClient.receiveMessage(qUrl);
		assertEquals(0, rMsgRes.getMessages().size());
	}
	
	@Ignore
	@Test
	public void testPublishTopTopic() throws JSONObjectAdapterException {
		remoteFilePreviewMsgPublisher.setPublishToQueueEnabled(true);
		RemoteFilePreviewGenerationRequest expectedReq = new RemoteFilePreviewGenerationRequest();
		S3FileHandle expectedSrc = new S3FileHandle();
		expectedSrc.setBucketName("srcBucketName");
		expectedReq.setSource(expectedSrc);
		S3FileHandle expectedDest = new S3FileHandle();
		expectedDest.setBucketName("destBucketName");
		expectedReq.setDestination(expectedDest);
		ArgumentCaptor<PublishRequest> captor1 = ArgumentCaptor.forClass(PublishRequest.class);
		remoteFilePreviewMsgPublisher.publishToQueue(expectedReq);
		ReceiveMessageResult rMsgRes = awsSQSClient.receiveMessage(qUrl);
		assertEquals(1, rMsgRes.getMessages().size());
	}

}
