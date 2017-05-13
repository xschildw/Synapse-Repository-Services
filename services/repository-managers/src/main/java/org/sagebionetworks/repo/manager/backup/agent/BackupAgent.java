package org.sagebionetworks.repo.manager.backup.agent;

import com.amazonaws.services.s3.AmazonS3Client;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.backup.daemon.BackupDriver;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.migration.MigrationType;

import java.io.File;
import java.util.List;

public class BackupAgent {

	static private Logger log = LogManager.getLogger(BackupAgent.class);

	private ProgressCallback<Void> callback;
	private UserInfo user;
	private BackupDriver driver;
	private AmazonS3Client s3Client;
	private String backupBucket;
	private MigrationType migrationType;
	private List<Long> backupIds;

	public BackupAgent(ProgressCallback<Void> callback, UserInfo user, BackupDriver driver, AmazonS3Client s3Client, String backupBucket, MigrationType type, List<Long> backupIds) {
		if (callback == null) throw new IllegalArgumentException("Callback cannot be null.");
		if (user == null) throw new IllegalArgumentException("User cannot be null.");
		if (driver ==  null) throw new IllegalArgumentException("Driver cannot be null.");
		if (s3Client == null) throw new IllegalArgumentException("S3Client cannot be null.");
		if (backupBucket == null) throw new IllegalArgumentException("BackupBucket cannot be null");
		if (type == null) throw new IllegalArgumentException("Type cannot be null");
		if (backupIds == null) throw new IllegalArgumentException("BackupsIds cannot be null");

		this.backupBucket = backupBucket;
		this.backupIds = backupIds;
		this.callback = callback;
		this.driver = driver;
		this.migrationType = type;
		this.s3Client = s3Client;
		this.user = user;

	}

	public void doBackup() {
		String stack = StackConfiguration.singleton().getStack();
		String instance = StackConfiguration.getStackInstance();
		final File tempBackup = File.createTempFile(stack+"-"+instance+"-"+status.getId()+"-", ".zip");
		tempToDelete = tempBackup;
		return;
	}
}
