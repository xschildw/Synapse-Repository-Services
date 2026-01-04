package org. sagebionetworks.simpleHttpClient;

import org.apache.http.HttpRequest;

import java.io.IOException;

public interface RequestSigner {
    /**
     * Signs the given HTTP request
     *
     * @param request the HTTP request to sign
     * @throws IOException if signing fails
     */
    public void signRequest(HttpRequest request) throws IOException;
}
