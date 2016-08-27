package org.sagebionetworks.migration.worker;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.Callable;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.asynch.AsynchJobStatusManager;
import org.sagebionetworks.repo.manager.migration.MigrationManager;
import org.sagebionetworks.repo.manager.migration.MigrationManagerSupport;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.migration.AsyncMigrationTypeCountRequest;
import org.sagebionetworks.repo.model.migration.AsyncMigrationTypeCountResult;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.migration.MigrationTypeCount;
import org.springframework.test.util.ReflectionTestUtils;

import com.amazonaws.services.sqs.model.Message;

public class AsyncMigrationRequestProcessorImplTest {

	@Mock
	AsynchJobStatusManager mockAsynchJobStatusManager;
	@Mock
	MigrationManager mockMigrationManager;
	@Mock
	ProgressCallback<Void> mockOuterCallback;
	@Mock
	MigrationManagerSupport mockMigrationManagerSupport;
	
	AsyncMigrationRequestProcessor processor;
	
	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		
		processor = new AsyncMigrationRequestProcessorImpl();
		ReflectionTestUtils.setField(processor, "asynchJobStatusManager", mockAsynchJobStatusManager);
		ReflectionTestUtils.setField(processor, "migrationManager", mockMigrationManager);
		ReflectionTestUtils.setField(processor,  "migrationManagerSupport", mockMigrationManagerSupport);
		
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

	@Test(expected=RuntimeException.class)
	public void testGetMigrationTypeCountNotHappy() throws Throwable {
		AsyncMigrationTypeCountRequest req = new AsyncMigrationTypeCountRequest();
		req.setType(MigrationType.NODE.name());
		UserInfo user = new UserInfo(true);
		user.setId(123L);
		String jobId = "123";
		when(mockMigrationManager.getMigrationTypeCount(eq(user), eq(MigrationType.NODE))).thenThrow(new RuntimeException());
		processor.processAsyncMigrationTypeCountRequest(mockOuterCallback, user, req, jobId);
		verify(mockAsynchJobStatusManager).setJobFailed(anyString(), any(Throwable.class));
	}
	
	@Test
	public void testGetMigrationTypeCountHappy() throws Throwable {
		AsyncMigrationTypeCountRequest req = new AsyncMigrationTypeCountRequest();
		req.setType(MigrationType.NODE.name());
		UserInfo user = new UserInfo(true);
		user.setId(123L);
		String jobId = "123";
		MigrationTypeCount expectedCount = new MigrationTypeCount();
		expectedCount.setType(MigrationType.NODE);
		expectedCount.setCount(10L);
		expectedCount.setMinid(101L);
		expectedCount.setMaxid(110L);
		when(mockMigrationManager.getMigrationTypeCount(eq(user), eq(MigrationType.NODE))).thenReturn(expectedCount);
		processor.processAsyncMigrationTypeCountRequest(mockOuterCallback, user, req, jobId);
		verify(mockAsynchJobStatusManager).setComplete(anyString(), any(AsyncMigrationTypeCountResult.class));
	}

}
