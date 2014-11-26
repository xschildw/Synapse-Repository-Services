package org.sagebionetworks.search;

import static org.junit.Assert.*;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.utils.HttpClientHelperException;

import com.amazonaws.http.HttpRequest;

import static org.mockito.Mockito.when;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class CloudSearchClientTest {
	
	CloudSearchHttpClientProvider mockHttpClientProvider;
	HttpClient mockHttpClient;
	CloudSearchClient cloudSearchClient;

	@Before
	public void before() {
		mockHttpClientProvider = Mockito.mock(CloudSearchHttpClientProvider.class);
		mockHttpClient = Mockito.mock(HttpClient.class);
		when(mockHttpClientProvider.getHttpClient()).thenReturn(mockHttpClient);
		cloudSearchClient = new CloudSearchClient(mockHttpClientProvider, "https://svc.endpoint.com", "https://doc.endpoint.com")
	;}
	
	@Test 
	public void testPLFM2968() throws Exception {
		//when(mockHttpClient.execute(any(HttpRequestBase.class))).thenThrow(new RuntimeException());
		StatusLine status = new BasicStatusLine(new ProtocolVersion("HTTP", 1, 1), 507, "SomeReason");
		HttpResponse resp = new BasicHttpResponse(status);
		when(mockHttpClient.execute(any(HttpRequestBase.class))).thenReturn(resp);
		cloudSearchClient.performSearch("aQuery");
	}

}
