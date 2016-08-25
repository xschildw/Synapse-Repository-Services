package org.sagebionetworks.repo.web.service;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.asynch.AsynchJobStatusManager;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.asynch.AsynchronousRequestBody;
import org.sagebionetworks.repo.model.migration.AsyncMigrationTypeCountRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.eq;

public class AsynchronousJobServicesImplTest {
	
	@Mock
	private UserManager mockUserManager;
	@Mock
	private AsynchJobStatusManager mockAsynchJobStatusManager;
	
	AsynchronousJobServicesImpl svc;

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		svc = new AsynchronousJobServicesImpl();
		ReflectionTestUtils.setField(svc, "userManager", mockUserManager);
		ReflectionTestUtils.setField(svc, "asynchJobStatusManager", mockAsynchJobStatusManager);
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test(expected=UnauthorizedException.class)
	public void testStartJobAsAdminUnauthorized() {
		Long userId = 123L;
		UserInfo expectedUser = new UserInfo(false);
		expectedUser.setId(123L);
		AsynchronousRequestBody body = new AsyncMigrationTypeCountRequest();
		when(mockUserManager.getUserInfo(eq(userId))).thenReturn(expectedUser);
		svc.startJobAsAdmin(userId, body);
	}
	
	@Test
	public void testStartJobAsAdmin() {
		Long userId = 123L;
		UserInfo expectedUser = new UserInfo(true);
		expectedUser.setId(123L);
		AsynchronousRequestBody body = new AsyncMigrationTypeCountRequest();
		when(mockUserManager.getUserInfo(eq(userId))).thenReturn(expectedUser);
		AsynchronousJobStatus expectedStatus = new AsynchronousJobStatus();
		expectedStatus.setJobId("jobId");
		when(mockAsynchJobStatusManager.startJob(eq(expectedUser), eq(body))).thenReturn(expectedStatus);
		AsynchronousJobStatus status = svc.startJobAsAdmin(userId, body);
		assertEquals(expectedStatus.getJobId(), status.getJobId());
	}

}
