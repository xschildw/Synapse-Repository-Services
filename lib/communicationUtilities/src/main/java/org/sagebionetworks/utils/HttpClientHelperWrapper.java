package org.sagebionetworks.utils;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;

public interface HttpClientHelperWrapper {
	public ThreadSafeClientConnManager createClientConnectionManager(boolean verifySSLCertificates) throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException, CertificateException, IOException;
	public String getContent(final HttpClient client, final String requestUrl) throws ClientProtocolException, IOException, HttpClientHelperException;
}
