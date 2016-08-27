package org.sagebionetworks.migration.worker;

import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.migration.AsyncMigrationTypeCountRequest;

public interface AsyncMigrationRequestProcessor {
	
	public void processAsyncMigrationTypeCountRequest(final ProgressCallback<Void> progressCallback, final UserInfo user, final AsyncMigrationTypeCountRequest mtcReq, final String jobId) throws Throwable;

}
