package org.sagebionetworks.repo.manager.backup.agent;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.backup.daemon.BackupDriver;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

public class BackupAgentFactoryImpl implements BackupAgentFactory {

	private static String backupBucket = StackConfiguration.getSharedS3BackupBucket();

	@Autowired
	BackupDriver backupDriver;

	private AmazonS3Client createNewAWSClient() {
		String iamId = StackConfiguration.getIAMUserId();
		String iamKey = StackConfiguration.getIAMUserKey();
		if (iamId == null) throw new IllegalArgumentException("IAM id cannot be null");
		if (iamKey == null)	throw new IllegalArgumentException("IAM key cannot be null");
		AWSCredentials creds = new BasicAWSCredentials(iamId, iamKey);
		AmazonS3Client client = new AmazonS3Client(creds);
		return client;
	}

	@Override
	public BackupAgent getBackupAgent(ProgressCallback<Void> callback, UserInfo user, MigrationType type, List<Long> backupIds) {
		AmazonS3Client s3Client = createNewAWSClient();
		BackupAgent backupAgent = new BackupAgent(callback, user, backupDriver, s3Client, backupBucket, type, backupIds);
		return backupAgent;
	}
}
