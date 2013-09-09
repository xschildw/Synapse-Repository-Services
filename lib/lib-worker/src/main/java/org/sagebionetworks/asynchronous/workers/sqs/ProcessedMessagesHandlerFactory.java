package org.sagebionetworks.asynchronous.workers.sqs;

import java.util.LinkedList;
import java.util.List;

public class ProcessedMessagesHandlerFactory {
	
	public ProcessedMessagesHandler createProcessedMessagesHandler() {
		return new ProcessedMessagesHandlerImpl();
	}
}
