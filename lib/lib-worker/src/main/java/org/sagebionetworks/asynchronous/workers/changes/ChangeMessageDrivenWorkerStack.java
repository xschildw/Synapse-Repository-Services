package org.sagebionetworks.asynchronous.workers.changes;

import org.sagebionetworks.database.semaphore.CountingSemaphore;
import org.sagebionetworks.workers.util.aws.message.MessageDrivenWorkerStack;

import software.amazon.awssdk.services.sqs.SqsClient;

/**
 * A worker stack driven by a Synapse change message.
 * 
 * @author jhill
 *
 */
public class ChangeMessageDrivenWorkerStack implements Runnable {

	MessageDrivenWorkerStack stack;

	public ChangeMessageDrivenWorkerStack(CountingSemaphore semaphore,
										  SqsClient sqsClient,
										  ChangeMessageDrivenWorkerStackConfig config) {
		// Get the configured runner.
		ChangeMessageRunner changeRunner = config.getRunner();
		if(changeRunner instanceof LockTimeoutAware){
			LockTimeoutAware lockAware = (LockTimeoutAware) changeRunner;
			// forward the lock timeout to the runner.
			lockAware.setTimeoutSeconds(config.getConfig().getSemaphoreGatedRunnerConfiguration().getLockTimeoutSec());
		}
		// Wrap the runner in a processor that converts batches of change
		// messages into single messages.
		ChangeMessageBatchProcessor batchProcessor = new ChangeMessageBatchProcessor(
				sqsClient, config.getConfig().getMessageQueueConfiguration()
						.getQueueName(), changeRunner);
		config.getConfig().setRunner(batchProcessor);
		stack = new MessageDrivenWorkerStack(semaphore, sqsClient, config.getConfig());
	}

	@Override
	public void run() {
		stack.run();
	}

}
