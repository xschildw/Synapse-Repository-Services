package org.sagebionetworks.migration.worker;

import java.io.IOException;
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
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.migration.*;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.workers.util.aws.message.MessageDrivenRunner;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.sqs.model.Message;

public class MigrationWorker implements MessageDrivenRunner {

	public static final long AUTO_PROGRESS_FREQUENCY_MS = 5*1000; // 5 seconds

	static private Logger log = LogManager.getLogger(MigrationWorker.class);

	@Autowired
	private AsynchJobStatusManager asynchJobStatusManager;
	@Autowired
	private UserManager userManager;
	@Autowired
	private MigrationManager migrationManager;
	@Autowired
	private ExecutorService migrationExecutorService;


	@Override
	public void run(ProgressCallback<Void> progressCallback, Message message)
			throws RecoverableMessageException, Exception {
		
		try {
			final AsynchronousJobStatus status = asynchJobStatusManager.lookupJobStatus(message.getBody());
			final UserInfo user = userManager.getUserInfo(status.getStartedByUserId());
			final AsyncMigrationRequest req = AsynchJobUtils.extractRequestBody(status, AsyncMigrationRequest.class);
			processAsyncMigrationRequest(progressCallback, user, req, status.getJobId());
		} catch (Throwable e) {
			log.error("Failed", e);
		}
	}
	
	public void processAsyncMigrationRequest(
			final ProgressCallback<Void> progressCallback, final UserInfo user,
			final AsyncMigrationRequest mReq, final String jobId) throws Throwable {

		try {
			final AdminRequest adminRequest = mReq.getAdminRequest();
			/*
				AsyncMigrationBackupRequest and AsyncMigrationRestoreRequest use ProgressingCallBack
			 */
			if ((adminRequest instanceof AsyncMigrationBackupRequest) || (adminRequest instanceof AsyncMigrationRestoreRequest)) {
				// TODO: Add RestoreRequest handling
				AsyncMigrationBackupRequest backupReq = (AsyncMigrationBackupRequest)adminRequest;
				AsyncMigrationResponse mResp = new AsyncMigrationResponse();
				MigrationBackupFileInfo backupFileInfo = migrationManager.processAsyncMigrationBackupRequest(progressCallback, user, backupReq);
				mResp.setAdminResponse(backupFileInfo);
				asynchJobStatusManager.setComplete(jobId, mResp);
			} else {
				callWithAutoProgress(progressCallback, new Callable<Void>() {
					@Override
					public Void call() throws Exception {
						AdminResponse resp = processRequest(user, adminRequest, jobId);
						AsyncMigrationResponse mResp = new AsyncMigrationResponse();
						mResp.setAdminResponse(resp);
						asynchJobStatusManager.setComplete(jobId, mResp);
						return null;
					}
				});
			}
		} catch (Throwable e) {
			// Record the error
			asynchJobStatusManager.setJobFailed(jobId, e);
			throw e;
		}
	}
	
	// TODO: Remove unused jobId param
	AdminResponse processRequest(final UserInfo user, final AdminRequest req, final String jobId) throws DatastoreException, NotFoundException, IOException {
		if (req instanceof AsyncMigrationTypeCountRequest) {
			return migrationManager.processAsyncMigrationTypeCountRequest(user, (AsyncMigrationTypeCountRequest)req);
		} else if (req instanceof AsyncMigrationTypeCountsRequest) {
			return migrationManager.processAsyncMigrationTypeCountsRequest(user, (AsyncMigrationTypeCountsRequest)req);
		} else if (req instanceof AsyncMigrationTypeChecksumRequest) {
			return migrationManager.processAsyncMigrationTypeChecksumRequest(user, (AsyncMigrationTypeChecksumRequest)req);
		} else if (req instanceof AsyncMigrationRangeChecksumRequest) {
			return migrationManager.processAsyncMigrationRangeChecksumRequest(user, (AsyncMigrationRangeChecksumRequest)req);
		} else if (req instanceof AsyncMigrationRowMetadataRequest) {
			return migrationManager.processAsyncMigrationRowMetadataRequest(user, (AsyncMigrationRowMetadataRequest)req);
		} else {
			throw new IllegalArgumentException("AsyncMigrationRequest not supported.");
		}
	}

	private <R> R callWithAutoProgress(ProgressCallback<Void> callback, Callable<R> callable) throws Exception {
		AutoProgressingCallable<R> auto = new AutoProgressingCallable<R>(
				migrationExecutorService, callable, AUTO_PROGRESS_FREQUENCY_MS);
		return auto.call(callback);
	}

}
