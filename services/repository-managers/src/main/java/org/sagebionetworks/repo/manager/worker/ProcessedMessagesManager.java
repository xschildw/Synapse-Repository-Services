package org.sagebionetworks.repo.manager.worker;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.sagebionetworks.asynchronous.workers.sqs.MessageUtils;
import org.sagebionetworks.asynchronous.workers.sqs.ProcessedMessagesHandler;
import org.sagebionetworks.cloudwatch.Consumer;
import org.sagebionetworks.cloudwatch.ProfileData;
import org.sagebionetworks.repo.model.ProcessedMessageDAO;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.sqs.model.Message;

public class ProcessedMessagesManager implements ProcessedMessagesHandler {

	@Autowired
	ProcessedMessageDAO processedMsgMgr;
	
	/**
	 * Register processed messages
	 */
	@Override
	public void handleProcessedMessages(List<Message> processedMsgs, String qName) {
		List<Long> l = new LinkedList<Long>();
		for (Message m: processedMsgs) {
			ChangeMessage chgMsg = MessageUtils.extractMessageBody(m);
			processedMsgMgr.registerMessageProcessed(chgMsg.getChangeNumber(), qName);
		}
	}
	
}
