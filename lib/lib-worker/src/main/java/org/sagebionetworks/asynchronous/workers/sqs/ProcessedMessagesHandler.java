package org.sagebionetworks.asynchronous.workers.sqs;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.sagebionetworks.cloudwatch.Consumer;
import org.sagebionetworks.cloudwatch.ProfileData;
import org.sagebionetworks.repo.model.ProcessedMessageDAO;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.sqs.model.Message;

public class ProcessedMessagesHandler {

	@Autowired
	ProcessedMessageDAO processedMsgMgr;
	
	@Autowired
	private Consumer consumer;
	
	protected static ProcessedMessagesHandler instance = null;
	
	protected ProcessedMessagesHandler() {
		
	}
	
	public static ProcessedMessagesHandler getInstance() {
		if (instance == null) {
			instance = new ProcessedMessagesHandler();
		}
		return instance;
	}

	/**
	 * Register processed messages
	 */
	public List<Long> registerProcessedMessages(List<Message> processedMsgs, String qName) {
		List<Long> l = new LinkedList<Long>();
		for (Message m: processedMsgs) {
			ChangeMessage chgMsg = MessageUtils.extractMessageBody(m);
			Long cn = processedMsgMgr.registerMessageProcessed(chgMsg.getChangeNumber(), qName);
			if (cn != null) {
				l.add(cn);
			}
		}
		return l;
	}
	
	/**
	 * Send processing times to CloudWatch
	 */
	public void sendProcessingTimesToCloudWatch(List<Long> processingTimes, String qName) {
		for (Long t: processingTimes) {
			addMetric(t, qName);
		}
	}
	
	private void addMetric(Long pTime, String qName) {
		ProfileData profileData = new ProfileData();
		profileData.setNamespace("MessageReceiver");
		profileData.setName(qName);
		profileData.setLatency(pTime);
		profileData.setUnit("second");
		profileData.setTimestamp(new Date());
		consumer.addProfileData(profileData);
	}
}
