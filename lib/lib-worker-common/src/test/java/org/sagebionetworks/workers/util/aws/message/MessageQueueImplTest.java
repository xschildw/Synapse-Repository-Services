package org.sagebionetworks.workers.util.aws.message;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlResponse;

@ExtendWith(MockitoExtension.class)
public class MessageQueueImplTest {

	@Mock
	SqsClient mockSQSClient;

	String queueUrl;
	MessageQueueConfiguration config;

	@BeforeEach
	public void setUp() throws Exception {
		queueUrl = "queueURL";

		// config
		config = new MessageQueueConfiguration();
		config.setQueueName("queueName");
		config.setEnabled(true);

		when(mockSQSClient.getQueueUrl(any(GetQueueUrlRequest.class))).thenReturn(GetQueueUrlResponse.builder().queueUrl(queueUrl).build());
	}

	@Test
	public void testConstructor() {
		config = new MessageQueueConfiguration();
		String queueName = "queueName";
		config.setQueueName(queueName);
		config.setEnabled(true);
		// call under test
		MessageQueueImpl msgQImpl = new MessageQueueImpl(mockSQSClient, config);
		assertEquals(queueUrl, msgQImpl.getQueueUrl());
		verify(mockSQSClient).getQueueUrl(GetQueueUrlRequest.builder().queueName(queueName).build());
		verifyNoMoreInteractions(mockSQSClient);
	}
}
