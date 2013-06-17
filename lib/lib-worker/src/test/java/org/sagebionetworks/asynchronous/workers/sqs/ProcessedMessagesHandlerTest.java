package org.sagebionetworks.asynchronous.workers.sqs;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.repo.model.ProcessedMessageDAO;
import org.springframework.dao.DataAccessException;

import static org.mockito.Mockito.mock;

public class ProcessedMessagesHandlerTest {
	
	ProcessedMessageDAO mockProcessedMessageDAO = Mockito.mock(ProcessedMessageDAO.class);

	@Test
	public void testRegisterProcessedMessages() {
		
	}
	
	@Test(expected=DataAccessException.class)
	public void testRegisterProcessedMessagesKeyViolation() {
		
	}

	@Test
	public void testListMessagesNotProcessed() {
		
	}
	
}
