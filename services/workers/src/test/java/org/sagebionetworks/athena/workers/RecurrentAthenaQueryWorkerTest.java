package org.sagebionetworks.athena.workers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.athena.RecurrentAthenaQueryManager;
import org.sagebionetworks.repo.model.athena.RecurrentAthenaQueryResult;
import org.sagebionetworks.util.progress.ProgressCallback;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;

import com.amazonaws.services.sqs.model.Message;

import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlResponse;

@ExtendWith(MockitoExtension.class)
public class RecurrentAthenaQueryWorkerTest {
	
	@Mock
	private ProgressCallback mockCallback;
	
	@Mock
	private Message mockMessage;
	
	@Mock
	private SqsClient mockSqsClient;
	
	@Mock
	private RecurrentAthenaQueryManager mockManager;
	
	@InjectMocks
	private RecurrentAthenaQueryWorker worker;
	
	@Mock
	private RecurrentAthenaQueryResult mockResult;
	
	private String queueUrl;
	
	@BeforeEach
	public void before() {
		this.queueUrl = "queueUrl";
		this.worker.setQueueName("queueName");		
		when(mockSqsClient.getQueueUrl(any(GetQueueUrlRequest.class))).thenReturn(GetQueueUrlResponse.builder().queueUrl(queueUrl).build());
		this.worker.configure();
	}

	@Test
	public void testRun() throws RecoverableMessageException {
		
		// Call under test
		worker.run(mockCallback, mockMessage, mockResult);
		
		verify(mockManager).processRecurrentAthenaQueryResult(mockResult, queueUrl);
		verify(mockSqsClient).getQueueUrl(GetQueueUrlRequest.builder().queueName("queueName").build());
	}

}
