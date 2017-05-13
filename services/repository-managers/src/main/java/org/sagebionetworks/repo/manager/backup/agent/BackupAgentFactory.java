package org.sagebionetworks.repo.manager.backup.agent;

import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.migration.MigrationType;

import java.util.List;

public interface BackupAgentFactory {

	BackupAgent getBackupAgent(ProgressCallback<Void> callback, UserInfo user, MigrationType type, List<Long> backupIds);

}
