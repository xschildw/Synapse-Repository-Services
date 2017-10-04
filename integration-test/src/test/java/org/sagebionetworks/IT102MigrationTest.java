package org.sagebionetworks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.*;

import io.jsonwebtoken.lang.Strings;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.SynapseAdminClientImpl;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.IdList;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.asynch.AsynchJobState;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.asynch.AsynchronousResponseBody;
import org.sagebionetworks.repo.model.daemon.BackupRestoreStatus;
import org.sagebionetworks.repo.model.daemon.DaemonStatus;
import org.sagebionetworks.repo.model.daemon.RestoreSubmission;
import org.sagebionetworks.repo.model.message.FireMessagesResult;
import org.sagebionetworks.repo.model.migration.*;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

public class IT102MigrationTest {

	private static SynapseAdminClient adminSynapse;
	private Project project;
	private static final long ASYNC_MIGRATION_MAX_WAIT_MS = 20000;
	
	private List<Entity> toDelete;

	@BeforeClass
	public static void beforeClass() throws Exception {
		adminSynapse = new SynapseAdminClientImpl();
		SynapseClientHelper.setEndpoints(adminSynapse);
		adminSynapse.setUsername(StackConfiguration.getMigrationAdminUsername());
		adminSynapse.setApiKey(StackConfiguration.getMigrationAdminAPIKey());
		adminSynapse.clearAllLocks();
	}
	
	@Before
	public void before() throws Exception {
		adminSynapse.clearAllLocks();
		toDelete = new ArrayList<Entity>();
		project = new Project();
		project.setName("projectIT102");
		project = adminSynapse.createEntity(project);
		toDelete.add(project);
	}

	@After
	public void after() throws Exception {
		if(adminSynapse != null && toDelete != null){
			for(Entity e: toDelete){
				adminSynapse.deleteAndPurgeEntity(e);
			}
		}
	}

	private BackupRestoreStatus waitForDaemonCompletion(BackupRestoreStatus brStatus) throws InterruptedException, JSONObjectAdapterException, SynapseException {
		int loopCount = 1;
		while (brStatus.getStatus() != DaemonStatus.COMPLETED) {
			System.out.println("\t" + brStatus.getProgresssMessage());
			System.out.println("\tProgress:\t" + brStatus.getProgresssCurrent());
			Thread.sleep(1000L);
			brStatus = adminSynapse.getStatus(brStatus.getId());
			loopCount++;
			if (loopCount > 10) {
				throw new RuntimeException("Backup/Restore should have completed by now...");
			}
			if (brStatus.getStatus() == DaemonStatus.FAILED) {
				throw new RuntimeException("Backup failed...");
			}
		}
		return brStatus;
	}
	
	/**
	 * Extract the filename from the full url.
	 * @param fullUrl
	 * @return
	 */
	public String getFileNameFromUrl(String fullUrl){;
		int index = fullUrl.lastIndexOf("/");
		return fullUrl.substring(index+1, fullUrl.length());
	}
	
	@Test
	public void testRoundTrip() throws Exception {
		// Primary types
		System.out.println("Migration types");
		MigrationTypeList mtList = adminSynapse.getPrimaryTypes();
		List<MigrationType> migrationTypes = mtList.getList();
		Map<MigrationType, Long> countByMigrationType = new HashMap<MigrationType, Long>();
		for (MigrationType mt: migrationTypes) {
			System.out.println(mt.name());
			countByMigrationType.put(mt, 0L);
		}
		// Counts per type
		System.out.println("Counts by type");
		MigrationTypeCounts mtCounts = adminSynapse.getTypeCounts();
		List<MigrationTypeCount> mtcs = mtCounts.getList();
		for (MigrationTypeCount mtc: mtcs) {
			System.out.println(mtc.getType().name() + ":" + mtc.getCount());
			countByMigrationType.put(mtc.getType(), mtc.getCount());
		}
		// Checksums per type
		System.out.println("Checksums by type");
		String salt = "SALT";
		for (MigrationType mt: migrationTypes) {
			MigrationRangeChecksum mtc = adminSynapse.getChecksumForIdRange(mt, salt, 0L, Long.MAX_VALUE);
			System.out.println(mt.name() + ":" + mtc);			
		}
		
		// Round trip
		System.out.println("Backup/restore");
		IdList idList = new IdList();
		List<Long> ids = new ArrayList<Long>();
		ids.add(Long.parseLong(project.getId().substring(3)));
		idList.setList(ids);
		System.out.println("Backing up...");
		BackupRestoreStatus brStatus = adminSynapse.startBackup(MigrationType.NODE, idList);
		brStatus = waitForDaemonCompletion(brStatus);
		adminSynapse.deleteAndPurgeEntity(project);
		System.out.println("Restoring " + brStatus.getBackupUrl() + "...");
		String fName = getFileNameFromUrl(brStatus.getBackupUrl());
		RestoreSubmission rReq = new RestoreSubmission();
		rReq.setFileName(fName);
		brStatus = adminSynapse.startRestore(MigrationType.NODE, rReq);
		brStatus = waitForDaemonCompletion(brStatus);
		Project rp = adminSynapse.getEntity(project.getId(), Project.class);
		assertNotNull(rp);
		assertEquals(project.getId(), rp.getId());
		assertEquals(project.getName(), rp.getName());
		
		// Fire change messages
		FireMessagesResult fmRes = adminSynapse.fireChangeMessages(0L, 10L);
		assertTrue(fmRes.getNextChangeNumber() > 0);
	}

	@Test
	public void testAsyncCounts() throws Exception {

		AsyncMigrationTypeCountsRequest tcReq = new AsyncMigrationTypeCountsRequest();
		tcReq.setTypes(adminSynapse.getPrimaryTypes().getList());
		AsyncMigrationRequest migReq = new AsyncMigrationRequest();
		migReq.setAdminRequest(tcReq);

		AsynchronousJobStatus status = adminSynapse.startAdminAsynchronousJob(migReq);
		status = waitForJob(adminSynapse, status.getJobId());
		AsynchronousResponseBody body = status.getResponseBody();
		assertNotNull(body);
		AdminResponse response = unpackAsynchResponseBody(body);
		assertTrue(response instanceof MigrationTypeCounts);
	}

	@Test
	public void testAsyncMetadata() throws Exception {

		AsyncMigrationRowMetadataRequest mReq = new AsyncMigrationRowMetadataRequest();
		mReq.setType(MigrationType.NODE.name());
		mReq.setMinId(0L);
		mReq.setMaxId(Long.parseLong(Strings.replace(project.getId(), "syn", "")));
		mReq.setLimit(10L);
		mReq.setOffset(0L);
		AsyncMigrationRequest migReq = new AsyncMigrationRequest();
		migReq.setAdminRequest(mReq);

		AsynchronousJobStatus status = adminSynapse.startAdminAsynchronousJob(migReq);
		status = waitForJob(adminSynapse, status.getJobId());
		AsynchronousResponseBody body = status.getResponseBody();
		assertNotNull(body);
		AdminResponse response = unpackAsynchResponseBody(body);
		assertTrue(response instanceof RowMetadataResult);
	}

	@Test
	public void testAsyncRangeChecksum() throws Exception {
		AsyncMigrationRangeChecksumRequest rcReq = new AsyncMigrationRangeChecksumRequest();
		rcReq.setType(MigrationType.NODE.name());
		rcReq.setSalt("SALT");
		rcReq.setMinId(0L);
		rcReq.setMaxId(Long.parseLong(Strings.replace(project.getId(), "syn", "")));
		AsyncMigrationRequest migReq = new AsyncMigrationRequest();
		migReq.setAdminRequest(rcReq);

		AsynchronousJobStatus status = adminSynapse.startAdminAsynchronousJob(migReq);
		status = waitForJob(adminSynapse, status.getJobId());
		AsynchronousResponseBody body = status.getResponseBody();
		assertNotNull(body);
		AdminResponse response = unpackAsynchResponseBody(body);
		assertTrue(response instanceof MigrationRangeChecksum);
	}

	@Test
	public void testAsyncTypeChecksum() throws Exception {
		AsyncMigrationTypeChecksumRequest tckReq = new AsyncMigrationTypeChecksumRequest();
		tckReq.setType(MigrationType.NODE.name());
		AsyncMigrationRequest migReq = new AsyncMigrationRequest();
		migReq.setAdminRequest(tckReq);

		AsynchronousJobStatus status = adminSynapse.startAdminAsynchronousJob(migReq);
		status = waitForJob(adminSynapse, status.getJobId());
		AsynchronousResponseBody body = status.getResponseBody();
		assertNotNull(body);
		AdminResponse response = unpackAsynchResponseBody(body);
		assertTrue(response instanceof MigrationTypeChecksum);
	}

	private static AdminResponse unpackAsynchResponseBody(AsynchronousResponseBody body) {
		AsyncMigrationResponse resp;
		if (body instanceof AsyncMigrationResponse) {
			resp = (AsyncMigrationResponse)body;
			AdminResponse actualResponse = resp.getAdminResponse();
			return actualResponse;
		} else {
			throw new IllegalArgumentException("Body is not AsyncMigrationResponse");
		}
	}
	
	@Test
	public void testChecksumForIdRange() throws SynapseException, JSONObjectAdapterException {
		Long minId = Long.parseLong(project.getId().substring(3));
		Long maxId = Long.parseLong(project.getId().substring(3));
		String salt = "SALT";
		MigrationRangeChecksum checksum1 = adminSynapse.getChecksumForIdRange(MigrationType.NODE, salt, minId, maxId);
		assertNotNull(checksum1);
	}

	public static AsynchronousJobStatus waitForJob(SynapseAdminClient client, String jobId) throws Exception {
		long startTime = System.currentTimeMillis();
		AsynchronousJobStatus status = client.getAdminAsynchronousJobStatus(jobId);
		while ((status != null) && (! status.getJobState().equals(AsynchJobState.COMPLETE))) {
			System.out.println("Waiting for job to complete");
			Thread.sleep(2500L);
			status = client.getAdminAsynchronousJobStatus(jobId);
			if ((status != null) && (status.getJobState().equals(AsynchJobState.FAILED))) {
				throw new RuntimeException("Job Failed");
			}
			if (System.currentTimeMillis()-startTime > 30000) {
				throw new RuntimeException("Job timed out");
			}
		}
		System.out.println("Async migration job completed!");
		return status;
	}
	
}
