package org.sagebionetworks.table.worker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.SemaphoreManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.asynch.AsynchJobStatusManager;
import org.sagebionetworks.repo.manager.table.ColumnModelManager;
import org.sagebionetworks.repo.manager.table.TableRowManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.asynch.AsynchJobState;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.RowReferenceSet;
import org.sagebionetworks.repo.model.table.TableEntity;
import org.sagebionetworks.repo.model.table.TableRowChange;
import org.sagebionetworks.repo.model.table.UploadToTableRequest;
import org.sagebionetworks.repo.model.table.UploadToTableResult;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import au.com.bytecode.opencsv.CSVWriter;

import com.amazonaws.services.s3.AmazonS3Client;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class TableCSVAppenderWorkerIntegrationTest {
	
	public static final int MAX_WAIT_MS = 1000*60;
	@Autowired
	AsynchJobStatusManager asynchJobStatusManager;
	@Autowired
	StackConfiguration config;
	@Autowired
	FileHandleDao fileHandleDao;
	@Autowired
	EntityManager entityManager;
	@Autowired
	TableRowManager tableRowManager;
	@Autowired
	ColumnModelManager columnManager;
	@Autowired
	UserManager userManager;
	@Autowired
	AmazonS3Client s3Client;
	@Autowired
	SemaphoreManager semphoreManager;
	private UserInfo adminUserInfo;
	RowReferenceSet referenceSet;
	List<ColumnModel> schema;
	List<String> headers;
	private String tableId;
	private List<String> toDelete;
	private File tempFile;
	S3FileHandle fileHandle;
	
	@Before
	public void before() throws NotFoundException{
		// Only run this test if the table feature is enabled.
		Assume.assumeTrue(config.getTableEnabled());
		semphoreManager.releaseAllLocksAsAdmin(new UserInfo(true));
		// Start with an empty queue.
		asynchJobStatusManager.emptyAllQueues();
		// Get the admin user
		adminUserInfo = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.TEST_ADMIN_USER.getPrincipalId());
		toDelete = new LinkedList<String>();
	}
	
	@After
	public void after(){
		if(config.getTableEnabled()){
			if(adminUserInfo != null){
				for(String id: toDelete){
					try {
						entityManager.deleteEntity(adminUserInfo, id);
					} catch (Exception e) {}
				}
			}
			if(tempFile != null){
				tempFile.delete();
			}
			if(fileHandle != null){
				s3Client.deleteObject(fileHandle.getBucketName(), fileHandle.getKey());
				fileHandleDao.delete(fileHandle.getId());
			}
		}
	}

	@Test
	public void testRoundTrip() throws DatastoreException, InvalidModelException, UnauthorizedException, NotFoundException, IOException, InterruptedException{
		this.schema = new LinkedList<ColumnModel>();
		// Create a project
		Project project = new Project();
		project.setName(UUID.randomUUID().toString());
		String id = entityManager.createEntity(adminUserInfo, project, null);
		project = entityManager.getEntity(adminUserInfo, id, Project.class);
		toDelete.add(project.getId());
		// Create a few columns
		// String
		ColumnModel cm = new ColumnModel();
		cm.setColumnType(ColumnType.STRING);
		cm.setName("somestrings");
		cm = columnManager.createColumnModel(adminUserInfo, cm);
		this.schema.add(cm);
		// integer
		cm = new ColumnModel();
		cm.setColumnType(ColumnType.INTEGER);
		cm.setName("someinteger");
		cm = columnManager.createColumnModel(adminUserInfo, cm);
		schema.add(cm);
		headers = TableModelUtils.getHeaders(schema);
		
		// Create the table
		TableEntity table = new TableEntity();
		table.setParentId(project.getId());
		table.setColumnIds(headers);
		table.setName(UUID.randomUUID().toString());
		tableId = entityManager.createEntity(adminUserInfo, table, null);
		// Bind the columns. This is normally done at the service layer but the workers cannot depend on that layer.
		columnManager.bindColumnToObject(adminUserInfo, headers, tableId, true);
		
		// Create a CSV file to upload
		this.tempFile = File.createTempFile("TableCSVAppenderWorkerIntegrationTest", ".csv");
		CSVWriter csv = new CSVWriter(new FileWriter(tempFile));
		int rowCount = 100;
		try{
			// Write the header
			csv.writeNext(new String[]{schema.get(1).getName(), schema.get(0).getName()});
			// Write some rows
			for(int i=0; i<rowCount; i++){
				csv.writeNext(new String[]{""+i, "stringdata"+i});
			}
		}finally{
			csv.close();
		}
		fileHandle = new S3FileHandle();
		fileHandle.setBucketName(StackConfiguration.getS3Bucket());
		fileHandle.setKey(UUID.randomUUID()+".csv");
		fileHandle.setContentType("text/csv");
		fileHandle.setCreatedBy(""+adminUserInfo.getId());
		fileHandle.setFileName("ToAppendToTable.csv");
		// Upload the File to S3
		fileHandle = fileHandleDao.createFile(fileHandle, false);
		// Upload the file to S3.
		s3Client.putObject(fileHandle.getBucketName(), fileHandle.getKey(), this.tempFile);
		// We are now ready to start the job
		UploadToTableRequest body = new UploadToTableRequest();
		body.setTableId(tableId);
		body.setUploadFileHandleId(fileHandle.getId());
		AsynchronousJobStatus status = asynchJobStatusManager.startJob(adminUserInfo, body);
		// Wait for the job to complete.
		status = waitForStatus(adminUserInfo, status);
		assertNotNull(status);
		assertNotNull(status.getResponseBody());
		assertTrue(status.getResponseBody() instanceof UploadToTableResult);
		UploadToTableResult response = (UploadToTableResult) status.getResponseBody();
		assertNotNull(response.getEtag());
		assertEquals(new Long(rowCount), response.getRowsProcessed());
		// There should be one change set applied to the table
		List<TableRowChange> changes = this.tableRowManager.listRowSetsKeysForTable(tableId);
		assertNotNull(changes);
		assertEquals(1, changes.size());
		TableRowChange change = changes.get(0);
		assertEquals(new Long(rowCount), change.getRowCount());
		// the etag of the change should match what the job returned.
		assertEquals(change.getEtag(), response.getEtag());
	}

	private AsynchronousJobStatus waitForStatus(UserInfo user, AsynchronousJobStatus status) throws InterruptedException, DatastoreException,
			NotFoundException {
		long start = System.currentTimeMillis();
		while(!AsynchJobState.COMPLETE.equals(status.getJobState())){
			assertFalse("Job Failed: "+status.getErrorDetails(), AsynchJobState.FAILED.equals(status.getJobState()));
			System.out.println("Waiting for job to complete: Message: "+status.getProgressMessage()+" progress: "+status.getProgressCurrent()+"/"+status.getProgressTotal());
			assertTrue("Timed out waiting for table status",(System.currentTimeMillis()-start) < MAX_WAIT_MS);
			Thread.sleep(1000);
			// Get the status again 
			status = this.asynchJobStatusManager.getJobStatus(user, status.getJobId());
		}
		return status;
	}
}
