package org.sagebionetworks.asynchronous.workers.sqs;

import java.util.List;

import org.sagebionetworks.repo.model.ProcessedMessageDAO;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.sqs.model.Message;

public class ProcessedMessagesHandler {

	@Autowired
	ProcessedMessageDAO processedMsgMgr;

	List<Message> processedMessages;
	String queueName;
	
	ProcessedMessagesHandler(List<Message> processedMsgs, String qName) {
		this.processedMessages = processedMsgs;
		this.queueName = qName;
	}
	
	public void setProcessedMessages(List<Message> msgs) {
		this.processedMessages = msgs;
	}
	
	public void setQueueName(String name) {
		this.queueName = name;
	}
	
	/**
	 * Register all processed messages
	 */
	public int registerProcessedMessages() {
		for (Message m: processedMessages) {
			ChangeMessage chgMsg = MessageUtils.extractMessageBody(m);
			processedMsgMgr.registerMessageProcessed(chgMsg.getChangeNumber(), queueName);
		}
		return processedMessages.size();
	}
}
