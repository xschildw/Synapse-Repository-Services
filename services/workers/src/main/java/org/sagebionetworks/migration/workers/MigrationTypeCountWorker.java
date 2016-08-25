package org.sagebionetworks.migration.workers;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.common.util.progress.AutoProgressingCallable;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.asynch.AsynchJobStatusManager;
import org.sagebionetworks.repo.manager.asynch.AsynchJobUtils;
import org.sagebionetworks.repo.manager.migration.MigrationManager;
import org.sagebionetworks.repo.manager.migration.MigrationManagerSupport;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.migration.AsyncMigrationTypeCountRequest;
import org.sagebionetworks.repo.model.migration.AsyncMigrationTypeCountResult;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.migration.MigrationTypeCount;
import org.sagebionetworks.workers.util.aws.message.MessageDrivenRunner;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.sqs.model.Message;

public class MigrationTypeCountWorker implements MessageDrivenRunner {

	static private Logger log = LogManager.getLogger(MigrationTypeCountWorker.class);

	@Autowired
	private AsynchJobStatusManager asynchJobStatusManager;
	@Autowired
	private UserManager userManager;
	@Autowired
	private MigrationManager migrationManager;
	@Autowired
	private MigrationManagerSupport migrationManagerSupport;

	@Override
	public void run(ProgressCallback<Void> progressCallback, Message message)
			throws RecoverableMessageException, Exception {
		
		// First read the body
		try {
			processStatus(progressCallback, message);
		} catch (Throwable e) {
			log.error("Failed", e);
		}
	}
	
	public void processStatus(final ProgressCallback<Void> progressCallback, final Message message) throws Throwable {
		final AsynchronousJobStatus status = asynchJobStatusManager.lookupJobStatus(message.getBody());
		try {
			this.dispatchProcessStatus(progressCallback, status);
		} catch (Throwable e) {
			// Record the error
			asynchJobStatusManager.setJobFailed(status.getJobId(), e);
			throw e;
		}
	}
	
	public void dispatchProcessStatus(final ProgressCallback<Void> progressCallback, final AsynchronousJobStatus status) throws Exception {
		final UserInfo user = userManager.getUserInfo(status.getStartedByUserId());
		final AsyncMigrationTypeCountRequest req = AsynchJobUtils.extractRequestBody(status, AsyncMigrationTypeCountRequest.class);
		final String t = req.getType();
		final MigrationType mt = MigrationType.valueOf(t);
		migrationManagerSupport.callWithAutoProgress(progressCallback, new Callable<Void>() {
				@Override
				public Void call() throws Exception {
					MigrationTypeCount mtc = migrationManager.getMigrationTypeCount(user, mt);
					AsyncMigrationTypeCountResult res = new AsyncMigrationTypeCountResult();
					res.setCount(mtc);
					asynchJobStatusManager.setComplete(status.getJobId(), res);
					return null;
				}
			});
	}
	

}
