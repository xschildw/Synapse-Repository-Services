package org.sagebionetworks.asynchronous.workers.sqs;

import java.util.LinkedList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.sqs.model.Message;

import org.sagebionetworks.repo.model.ProcessedMessageDAO;
import org.sagebionetworks.repo.model.message.ChangeMessage;

public class ProcessedMessagesHandlerImpl implements ProcessedMessagesHandler {
	
	@Autowired
	ProcessedMessageDAO processedMessagesDao;

	@Override
	public void handleProcessedMessages(List<Message> processedMessages, String qName) {
		List<Long> l = new LinkedList<Long>();
		for (Message m: processedMessages) {
			ChangeMessage chgMsg = MessageUtils.extractMessageBody(m);
			processedMessagesDao.registerMessageProcessed(chgMsg.getChangeNumber(), qName);
		}
	}

}
