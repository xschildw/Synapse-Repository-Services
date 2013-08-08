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

public interface ProcessedMessagesHandler {

	/**
	 * Register processed messages
	 */
	public List<Long> registerProcessedMessages(List<Message> processedMsgs, String qName);
	
	/**
	 * Send processing times to CloudWatch
	 */
	public void sendProcessingTimesToCloudWatch(List<Long> processingTimes, String qName);

}
