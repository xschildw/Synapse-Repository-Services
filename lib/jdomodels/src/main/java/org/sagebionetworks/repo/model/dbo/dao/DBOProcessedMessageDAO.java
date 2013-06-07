package org.sagebionetworks.repo.model.dbo.dao;

import java.util.List;

import org.sagebionetworks.repo.model.message.ChangeMessage;

public interface DBOProcessedMessageDAO {

	/**
	 * Register a message that has been processed by a worker.
	 */
	public abstract void registerMessageProcessed(long changeNumber, String queueName);

	/**
	 * List messages that have been processed. This is used to detect messages that have been sent
	 * but not processed by a worker.
	 * @return
	 */
	public abstract List<ChangeMessage> listNotProcessedMessages(String queueName, long limit);

}