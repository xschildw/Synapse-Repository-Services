package org.sagebionetworks.migration.worker;

import static org.junit.Assert.*;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

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

import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.migration.worker.MigrationWorker;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.asynch.AsynchJobStatusManager;
import org.sagebionetworks.repo.manager.migration.MigrationManager;
import org.sagebionetworks.repo.manager.migration.MigrationManagerSupport;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.asynch.AsynchronousResponseBody;
import org.sagebionetworks.repo.model.migration.AsyncMigrationTypeCountRequest;
import org.sagebionetworks.repo.model.migration.AsyncMigrationTypeCountResult;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.migration.MigrationTypeCount;
import org.springframework.test.util.ReflectionTestUtils;

import com.amazonaws.services.sqs.model.Message;


public class MigrationWorkerTest {

	@Mock
	AsynchJobStatusManager mockAsynchJobStatusManager;
	@Mock
	UserManager mockUserManager;
	@Mock
	ProgressCallback<Void> mockOuterCallback;
	@Mock
	AsyncMigrationRequestProcessor mockRequestProcessor;
	
	MigrationWorker worker;
	UserInfo expectedUserInfo;
	AsyncMigrationTypeCountRequest req;
	
	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		
		worker = new MigrationWorker();
		ReflectionTestUtils.setField(worker, "asynchJobStatusManager", mockAsynchJobStatusManager);
		ReflectionTestUtils.setField(worker, "userManager", mockUserManager);
		ReflectionTestUtils.setField(worker,  "requestProcessor", mockRequestProcessor);
		
		AsynchronousJobStatus expectedJobStatus = new AsynchronousJobStatus();
		expectedJobStatus.setJobId("123");
		req = new AsyncMigrationTypeCountRequest();
		req.setType(MigrationType.NODE.name());
		expectedJobStatus.setRequestBody(req);
		final long userId = 123;
		expectedJobStatus.setStartedByUserId(userId);
		when(mockAsynchJobStatusManager.lookupJobStatus(eq("123"))).thenReturn(expectedJobStatus);
		
		expectedUserInfo = new UserInfo(false);
		expectedUserInfo.setId(userId);
		when(mockUserManager.getUserInfo(userId)).thenReturn(expectedUserInfo);
		
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void test() throws Throwable {
		Message msg = new Message().withBody("123");
		worker.run(mockOuterCallback, msg);
		verify(mockRequestProcessor).processAsyncMigrationTypeCountRequest(eq(mockOuterCallback), eq(expectedUserInfo), eq(req), eq("123"));
	}
	
}
