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
import org.sagebionetworks.migration.workers.MigrationTypeCountWorker;
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


public class MigrationTypeCountWorkerTest {

	@Mock
	AsynchJobStatusManager mockAsynchJobStatusManager;
	@Mock
	UserManager mockUserManager;
	@Mock
	MigrationManager mockMigrationManager;
	@Mock
	ProgressCallback<Void> mockOuterCallback;
	@Mock
	MigrationManagerSupport mockMigrationManagerSupport;
	
	MigrationTypeCountWorker worker;
	
	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		
		worker = new MigrationTypeCountWorker();
		ReflectionTestUtils.setField(worker, "asynchJobStatusManager", mockAsynchJobStatusManager);
		ReflectionTestUtils.setField(worker, "migrationManager", mockMigrationManager);
		ReflectionTestUtils.setField(worker, "userManager", mockUserManager);
		ReflectionTestUtils.setField(worker,  "migrationManagerSupport", mockMigrationManagerSupport);
		
		AsynchronousJobStatus expectedJobStatus = new AsynchronousJobStatus();
		expectedJobStatus.setJobId("123");
		AsyncMigrationTypeCountRequest req = new AsyncMigrationTypeCountRequest();
		req.setType(MigrationType.NODE.name());
		expectedJobStatus.setRequestBody(req);
		final long userId = 123;
		expectedJobStatus.setStartedByUserId(userId);
		when(mockAsynchJobStatusManager.lookupJobStatus(anyString())).thenReturn(expectedJobStatus);
		UserInfo expectedUserInfo = new UserInfo(false);
		expectedUserInfo.setId(userId);
		when(mockUserManager.getUserInfo(userId)).thenReturn(expectedUserInfo);
		MigrationTypeCount expectedCount = new MigrationTypeCount();
		expectedCount.setType(MigrationType.NODE);
		expectedCount.setCount(10L);
		expectedCount.setMinid(101L);
		expectedCount.setMaxid(110L);
		// when(mockMigrationManager.getMigrationTypeCount(eq(expectedUserInfo), eq(MigrationType.NODE))).thenReturn(expectedCount);
		when(mockMigrationManager.getMigrationTypeCount(eq(expectedUserInfo), eq(MigrationType.NODE))).thenThrow(new RuntimeException());
		when(mockMigrationManagerSupport.callWithAutoProgress(any(ProgressCallback.class), any(Callable.class))).then(new Answer<Object>() {

			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				Callable<Object> callable = (Callable<Object>) invocation.getArguments()[1];
				return callable.call();
			}
		});
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testNotHappy() throws Exception {
		Message msg = new Message().withBody("body");
		worker.run(mockOuterCallback, msg);
		//verify(mockAsynchJobStatusManager).setComplete(anyString(), any(AsyncMigrationTypeCountResult.class));
		verify(mockAsynchJobStatusManager).setJobFailed(anyString(), any(Throwable.class));
	}
	
}
