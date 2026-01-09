package org.sagebionetworks.simpleHttpClient;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.message.BasicHttpRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.regions.Region;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AwsSignatureV4SignerTest {

    private final String SERVICE_NAME = "service_name";
    private final Region REGION = Region.AP_NORTHEAST_1;

    @Mock
    AwsCredentials mockAwsCredentials;

    @BeforeEach
    void setUp() {
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    void testSignRequestNotHttpUri() throws IOException {

        BasicHttpRequest request = new BasicHttpRequest("GET", "/healeth");
        AwsSignatureV4Signer signer = new AwsSignatureV4Signer(mockAwsCredentials, REGION, SERVICE_NAME);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            signer.signRequest(request);
        } );
        assertEquals("Expected HttpUriRequest but got: class org.apache.http.message.BasicHttpRequest", ex.getMessage());

    }

    @Test
    void testSignRequestNullUri() throws IOException {

        HttpGet request = new HttpGet();
        AwsSignatureV4Signer signer = new AwsSignatureV4Signer(mockAwsCredentials, REGION, SERVICE_NAME);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            signer.signRequest(request);
        } );
        assertEquals("Request URI cannot be null.", ex.getMessage());

    }

    @Test
    void testSignRequestNullHost() throws IOException {

        HttpGet request = new HttpGet("/getHealth");
        AwsSignatureV4Signer signer = new AwsSignatureV4Signer(mockAwsCredentials, REGION, SERVICE_NAME);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            signer.signRequest(request);
        } );
        assertEquals("Request URI must include a host: /getHealth", ex.getMessage());

    }

    @Test
    void testSignRequestNoHeaders() throws IOException {

        when(mockAwsCredentials.accessKeyId()).thenReturn("fakeAccessKey");
        when(mockAwsCredentials.secretAccessKey()).thenReturn("fakeSecretKey");

        HttpGet request = new HttpGet("https://somehost.com/getHealth");
        AwsSignatureV4Signer signer = new AwsSignatureV4Signer(mockAwsCredentials, REGION, SERVICE_NAME);
        signer.signRequest(request);

        // Check Host header
        org.apache.http.Header[] hostHeaders = request.getHeaders("Host");
        assertEquals(1, hostHeaders.length, "Should have exactly one Host header");
        assertEquals("somehost.com", hostHeaders[0].getValue());

        // Check Authorization header
        org.apache.http.Header[] authHeaders = request.getHeaders("Authorization");
        assertEquals(1, authHeaders.length, "Should have exactly one Authorization header");
        assertTrue(authHeaders[0].getValue().startsWith("AWS4-HMAC-SHA256"), "Should be a SigV4 auth header");

        // Check Date header
        org.apache.http.Header[] dateHeaders = request.getHeaders("X-Amz-Date");
        assertEquals(1, dateHeaders.length, "Should have an X-Amz-Date header");
        assertNotNull(dateHeaders[0].getValue());

        // Important: Check that X-Amz-Security-Token is NOT present for basic credentials
        assertEquals(0, request.getHeaders("X-Amz-Security-Token").length, "Should not have a security token for basic credentials");
    }


}