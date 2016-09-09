package org.sagebionetworks.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.when;

import java.io.UnsupportedEncodingException;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.entity.StringEntity;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.asynch.AsyncJobId;
import org.sagebionetworks.repo.model.message.FireMessagesResult;
import org.sagebionetworks.repo.model.migration.AsyncMigrationRequest;
import org.sagebionetworks.repo.model.migration.AsyncMigrationResponse;
import org.sagebionetworks.repo.model.migration.AsyncMigrationTypeCountRequest;
import org.sagebionetworks.repo.model.migration.AsyncMigrationTypeCountResult;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.migration.MigrationTypeCount;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;


public class SynapseAdministrationTest {
	HttpClientProvider mockProvider = null;
	HttpResponse mockResponse;
	
	SynapseAdminClientImpl synapse;
	
	@Before
	public void before() throws Exception{
		// The mock provider
		mockProvider = Mockito.mock(HttpClientProvider.class);
		mockResponse = Mockito.mock(HttpResponse.class);
		when(mockProvider.performRequest(any(String.class),any(String.class),any(String.class),(Map<String,String>)anyObject())).thenReturn(mockResponse);
		synapse = new SynapseAdminClientImpl(mockProvider);
	}
	
	@Test
	public void testGetCurrentChangeNumber() throws Exception {
		FireMessagesResult expectedRes = new FireMessagesResult();
		expectedRes.setNextChangeNumber(-1L);
		String expectedJSONResult = EntityFactory.createJSONStringForEntity(expectedRes);
		StringEntity responseEntity = new StringEntity(expectedJSONResult);
		when(mockResponse.getEntity()).thenReturn(responseEntity);
		StatusLine statusLine = Mockito.mock(StatusLine.class);
		when(statusLine.getStatusCode()).thenReturn(200);
		when(mockResponse.getStatusLine()).thenReturn(statusLine);
		FireMessagesResult res = synapse.getCurrentChangeNumber();
		assertNotNull(res);
		assertEquals(expectedRes, res);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testBuildListMessagesURLNullStartNumber(){
		SynapseAdminClientImpl.buildListMessagesURL(null, ObjectType.EVALUATION, new Long(1));
	}
	@Test
	public void testBuildListMessagesURL(){
		String expected = "/admin/messages?startChangeNumber=345&type=EVALUATION&limit=987";
		String url = SynapseAdminClientImpl.buildListMessagesURL(new Long(345), ObjectType.EVALUATION, new Long(987));
		assertEquals(expected, url);
	}
	
	@Test
	public void testBuildListMessagesURLNullType(){
		String expected = "/admin/messages?startChangeNumber=345&limit=987";
		String url = SynapseAdminClientImpl.buildListMessagesURL(new Long(345),null, new Long(987));
		assertEquals(expected, url);
	}
	
	@Test
	public void testBuildListMessagesURLNullLimit(){
		String expected = "/admin/messages?startChangeNumber=345&type=EVALUATION";
		String url = SynapseAdminClientImpl.buildListMessagesURL(new Long(345), ObjectType.EVALUATION, null);
		assertEquals(expected, url);
	}
	
	@Test
	public void testBuildListMessagesURLAllNonRequiredNull(){
		String expected = "/admin/messages?startChangeNumber=345";
		String url = SynapseAdminClientImpl.buildListMessagesURL(new Long(345), null, null);
		assertEquals(expected, url);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testBuildPublishMessagesURLQueueNameNull(){
		SynapseAdminClientImpl.buildPublishMessagesURL(null, new Long(345), ObjectType.ACTIVITY, new Long(888));
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testBuildPublishMessagesURLStartNumberNull(){
		SynapseAdminClientImpl.buildPublishMessagesURL("some-queue", null, ObjectType.ACTIVITY, new Long(888));
	}
	
	@Test
	public void testBuildPublishMessagesURL(){
		String expected = "/admin/messages/rebroadcast?queueName=some-queue&startChangeNumber=345&type=ACTIVITY&limit=888";
		String url = SynapseAdminClientImpl.buildPublishMessagesURL("some-queue", new Long(345), ObjectType.ACTIVITY, new Long(888));
		assertEquals(expected, url);
	}
	
	@Test
	public void testBuildPublishMessagesURLTypeNull(){
		String expected = "/admin/messages/rebroadcast?queueName=some-queue&startChangeNumber=345&limit=888";
		String url = SynapseAdminClientImpl.buildPublishMessagesURL("some-queue", new Long(345), null, new Long(888));
		assertEquals(expected, url);
	}
	
	@Test
	public void testBuildPublishMessagesURLLimitNull(){
		String expected = "/admin/messages/rebroadcast?queueName=some-queue&startChangeNumber=345&type=ACTIVITY";
		String url = SynapseAdminClientImpl.buildPublishMessagesURL("some-queue", new Long(345), ObjectType.ACTIVITY, null);
		assertEquals(expected, url);
	}
	
	@Test
	public void testStartAsyncMigrationRequest() throws SynapseException, JSONObjectAdapterException, UnsupportedEncodingException {
		AsyncJobId expectedId = new AsyncJobId();
		expectedId.setToken("token");
		String expectedJSONRes = EntityFactory.createJSONStringForEntity(expectedId);
		StringEntity responseEntity = new StringEntity(expectedJSONRes);
		when(mockResponse.getEntity()).thenReturn(responseEntity);
		StatusLine statusLine = Mockito.mock(StatusLine.class);
		when(statusLine.getStatusCode()).thenReturn(200);
		when(mockResponse.getStatusLine()).thenReturn(statusLine);
		
		AsyncMigrationTypeCountRequest req = new AsyncMigrationTypeCountRequest();
		req.setType(MigrationType.NODE.name());
		
		AsyncJobId id = synapse.startAsyncMigrationRequest(req);
		
		assertNotNull(id);
		assertEquals(expectedId, id);
	}
	
	@Test
	public void testGetMigrationResponse() throws Exception {
		AsyncMigrationTypeCountResult expectedResult = new AsyncMigrationTypeCountResult();
		MigrationTypeCount mtc = new MigrationTypeCount();
		mtc.setType(MigrationType.NODE);
		mtc.setMinid(0L);
		mtc.setMaxid(100L);
		mtc.setCount(50L);
		expectedResult.setCount(mtc);
		String expectedJSONRes = EntityFactory.createJSONStringForEntity(expectedResult);
		StringEntity responseEntity = new StringEntity(expectedJSONRes);
		when(mockResponse.getEntity()).thenReturn(responseEntity);
		StatusLine statusLine = Mockito.mock(StatusLine.class);
		when(statusLine.getStatusCode()).thenReturn(200);
		when(mockResponse.getStatusLine()).thenReturn(statusLine);
		
		AsyncMigrationResponse response = synapse.getAsyncMigrationResponse("token");
		assertNotNull(response);
		if (! (response instanceof AsyncMigrationTypeCountResult)) {
			fail("Wrong type!");
		}
		
		
	}
}
