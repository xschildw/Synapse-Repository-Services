package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.manager.NodeManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.migration.MigrationManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.StackStatusDao;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.asynch.AsynchronousResponseBody;
import org.sagebionetworks.repo.model.migration.*;
import org.sagebionetworks.repo.model.status.StackStatus;
import org.sagebionetworks.repo.model.status.StatusEnum;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

public class AdministrationControllerTest extends AbstractAutowiredControllerTestBase {

	@Autowired
	public UserManager userManager;
	
	@Autowired
	public NodeManager nodeManager;
	
	@Autowired
	private StackStatusDao stackStatusDao;

	private List<String> toDelete;
	private Long adminUserId;
	private Project entity;

	@Before
	public void before() throws DatastoreException, NotFoundException {
		toDelete = new ArrayList<String>();
		adminUserId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
	}
	
	@After
	public void after() throws Exception {
		// Always restore the status to read-write
		StackStatus status = new StackStatus();
		status.setStatus(StatusEnum.READ_WRITE);
		stackStatusDao.updateStatus(status);

		
		UserInfo adminUserInfo = userManager.getUserInfo(adminUserId);
		
		if(entity != null){
			try {
				nodeManager.delete(adminUserInfo, entity.getId());
			} catch (DatastoreException e) {
				// nothing to do here
			} catch (NotFoundException e) {
				// nothing to do here
			}	
		}
		
		if (nodeManager != null && toDelete != null) {
			for (String idToDelete : toDelete) {
				try {
					nodeManager.delete(adminUserInfo, idToDelete);
				} catch (NotFoundException e) {
					// nothing to do here
				} catch (DatastoreException e) {
					// nothing to do here.
				}
			}
		}
	}
	
	@Test
	public void testGetStackStatus() throws Exception {
		// Make sure we can get the stack status
		StackStatus status = servletTestHelper.getStackStatus(dispatchServlet);
		assertNotNull(status);
		assertEquals(StatusEnum.READ_WRITE, status.getStatus());
	}
	
	@Test
	public void testUpdateStatus() throws Exception {
		// Make sure we can get the stack status
		StackStatus status = servletTestHelper.getStackStatus(dispatchServlet);
		assertNotNull(status);
		assertEquals(StatusEnum.READ_WRITE, status.getStatus());
		// Make sure we can update the status
		status.setPendingMaintenanceMessage("AdministrationControllerTest.testUpdateStatus");
		StackStatus back = servletTestHelper.updateStackStatus(dispatchServlet, adminUserId, status);
		assertEquals(status, back);
	}
	
	@Test
	public void testGetAndUpdateStatusWhenDown() throws Exception {
		// Make sure we can get the status when down.
		StackStatus setDown = new StackStatus();
		setDown.setStatus(StatusEnum.DOWN);
		setDown.setCurrentMessage("Synapse is going down for a test: AdministrationControllerTest.testGetStatusWhenDown");
		StackStatus back = servletTestHelper.updateStackStatus(dispatchServlet, adminUserId, setDown);
		assertEquals(setDown, back);
		// Make sure we can still get the status
		StackStatus current = servletTestHelper.getStackStatus(dispatchServlet);
		assertEquals(setDown, current);
		
		// Now make sure we can turn it back on when down.
		setDown.setStatus(StatusEnum.READ_WRITE);
		setDown.setCurrentMessage(null);
		back = servletTestHelper.updateStackStatus(dispatchServlet, adminUserId, setDown);
		assertEquals(setDown, back);
	}
	
	@Test
	public void testClearLocks() throws Exception{
		// Clear all locks
		servletTestHelper.clearAllLocks(dispatchServlet, adminUserId);
		
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testClearLocksUnauthorized() throws Exception{
		// Clear all locks
		servletTestHelper.clearAllLocks(dispatchServlet, BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId());
		
	}

	@Test
	public void testStartAdminAsyncJob() throws Exception {
		AsyncMigrationTypeCountsRequest tcReq = new AsyncMigrationTypeCountsRequest();
		List<MigrationType> types = Arrays.asList(MigrationType.ACCESS_APPROVAL);
		tcReq.setTypes(types);
		AsyncMigrationRequest req = new AsyncMigrationRequest();
		req.setAdminRequest(tcReq);
		AsynchronousJobStatus status1 = servletTestHelper.startAdminAsynchJob(dispatchServlet, adminUserId, req);
		assertNotNull(status1);
		assertNotNull(status1.getRequestBody());
		String jobId = status1.getJobId();
		assertNotNull(jobId);
		AsynchronousJobStatus status2 = servletTestHelper.getAdminAsynchJobStatus(dispatchServlet, adminUserId, jobId);
		assertEquals(status1, status2);
	}

	@Test
	public void testStartAdminAsyncJob2() throws Exception {
		AsyncMigrationRowMetadataRequest mReq = new AsyncMigrationRowMetadataRequest();
		mReq.setType(MigrationType.ACCESS_APPROVAL.name());
		mReq.setMaxId(100L);
		mReq.setMinId(0L);
		mReq.setLimit(10L);
		mReq.setOffset(0L);
		AsyncMigrationRequest req = new AsyncMigrationRequest();
		req.setAdminRequest(mReq);
		AsynchronousJobStatus status1 = servletTestHelper.startAdminAsynchJob(dispatchServlet, adminUserId, req);
		assertNotNull(status1);
		assertNotNull(status1.getRequestBody());
		String jobId = status1.getJobId();
		assertNotNull(jobId);
		AsynchronousJobStatus status2 = servletTestHelper.getAdminAsynchJobStatus(dispatchServlet, adminUserId, jobId);
		assertEquals(status1, status2);
	}

}

