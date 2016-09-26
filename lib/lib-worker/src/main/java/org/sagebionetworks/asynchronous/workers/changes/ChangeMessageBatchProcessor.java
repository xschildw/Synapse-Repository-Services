package org.sagebionetworks.asynchronous.workers.changes;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.asynchronous.workers.sqs.MessageUtils;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.workers.util.aws.message.MessageDrivenRunner;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;

import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.CreateQueueResult;
import com.amazonaws.services.sqs.model.Message;

/**
 * A message driven runner that can read a batch of change messages and
 * forwarded each message to the provided worker.
 * 
 * @author jhill
 *
 */
public class ChangeMessageBatchProcessor implements MessageDrivenRunner {

	static private Logger log = LogManager
			.getLogger(ChangeMessageBatchProcessor.class);

	private AmazonSQSClient awsSQSClient;
	private String queueUrl;
	private ChangeMessageRunner runner;

	public ChangeMessageBatchProcessor(AmazonSQSClient awsSQSClient,
			String queueName, ChangeMessageRunner runner) {
		this.awsSQSClient = awsSQSClient;
		// create the queue if it does not exist.
		CreateQueueResult result = awsSQSClient.createQueue(queueName);
		this.queueUrl = result.getQueueUrl();
		this.runner = runner;
	}

	@Override
	public void run(final ProgressCallback<Void> progressCallback,
			final Message message) throws RecoverableMessageException,
			Exception {
		// read the batch.
		List<ChangeMessage> batch = MessageUtils
				.extractChangeMessageBatch(message);
		if (runner instanceof BatchChangeMessageDrivenRunner) {
			BatchChangeMessageDrivenRunner batchRunner = (BatchChangeMessageDrivenRunner) runner;
			batchRunner.run(progressCallback, batch);
		} else if(runner instanceof ChangeMessageDrivenRunner) {
			ChangeMessageDrivenRunner singleRunner = (ChangeMessageDrivenRunner) runner;
			runAsSingleChangeMessages(progressCallback, batch, singleRunner);
		}else{
			throw new IllegalArgumentException("Unknown runner type: "+runner.getClass().getName());
		}
	}

	/**
	 * Run each messages from the batch separately.
	 * @param progressCallback
	 * @param batch
	 * @param runner
	 * @throws JSONObjectAdapterException
	 * @throws RecoverableMessageException
	 */
	void runAsSingleChangeMessages(
			final ProgressCallback<Void> progressCallback,
			List<ChangeMessage> batch, ChangeMessageDrivenRunner runner)
			throws JSONObjectAdapterException, RecoverableMessageException {
		// Run each batch
		for (ChangeMessage change : batch) {
			try {
				// Make progress before each message
				progressCallback.progressMade(null);
				runner.run(new ProgressCallback<Void>() {
					@Override
					public void progressMade(Void t) {
						progressCallback.progressMade(null);
					}
				}, change);
			} catch (RecoverableMessageException e) {
				if (batch.size() == 1) {
					// Let the container handle retry for single messages.
					throw e;
				} else {
					// Add the message back to the queue as a single message
					awsSQSClient.sendMessage(queueUrl,
							EntityFactory.createJSONStringForEntity(change));
				}
			} catch (Throwable e) {
				log.error(
						"Failed on Change Number: " + change.getChangeNumber(),
						e);
			}
		}
	}
}
