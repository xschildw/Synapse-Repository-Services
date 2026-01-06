package org.sagebionetworks.simpleHttpClient;

import org.apache.http.HttpRequest;
import org.apache.http.Header;
import org.apache.http.client.methods.HttpUriRequest;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.signer.Aws4Signer;
import software.amazon.awssdk.auth.signer.params.Aws4SignerParams;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.regions.Region;

import java.io.IOException;
import java.net.URI;

public class AwsSignatureV4Signer implements RequestSigner {
    private final AwsCredentials credentials;
    private final Region region;
    private final String serviceName;

    public AwsSignatureV4Signer(AwsCredentials credentials, Region region, String serviceName) {
        this.credentials = credentials;
        this. region = region;
        this. serviceName = serviceName;
    }

    @Override
    public void signRequest(HttpRequest apacheRequest) throws IOException {
        // Validation
        if (!(apacheRequest instanceof HttpUriRequest)) {
            throw new IllegalArgumentException("Expected HttpUriRequest but got: " + apacheRequest.getClass());
        }
        HttpUriRequest req = (HttpUriRequest) apacheRequest;
        URI uri = req.getURI();
        if (uri == null) {
            throw new IllegalArgumentException("Request URI cannot be null.");
        }

        String host = uri.getHost();
        if (host == null || host.trim().isEmpty()) {
            throw new IllegalArgumentException("Request URI must include a host: " + uri);
        }

        // Headers
        SdkHttpFullRequest.Builder sdkReq = SdkHttpFullRequest.builder()
                .method(SdkHttpMethod.fromValue(req.getMethod()))
                .uri(uri);

        // Copy headers (except auth headers we'll overwrite)
        for (Header h : req.getAllHeaders()) {
            String name = h.getName();
            if (name.equalsIgnoreCase("Authorization")
                    || name.equalsIgnoreCase("X-Amz-Date")
                    || name.equalsIgnoreCase("X-Amz-Security-Token")) {
                continue;
            }
            sdkReq.appendHeader(name, h.getValue());
        }

        // Ensure Host is present (SigV4 requires it)
        sdkReq.putHeader("Host", host);

        // IMPORTANT for streaming bodies: don't force reading the entity to hash it.
        sdkReq.putHeader("x-amz-content-sha256", "UNSIGNED-PAYLOAD");

        Aws4Signer signer = Aws4Signer.create();
        Aws4SignerParams signerParams = Aws4SignerParams.builder()
                .awsCredentials(credentials)
                .signingName(serviceName)
                .signingRegion(region)
                .build();

        // Build and sign the request
        SdkHttpFullRequest sdkRequest = sdkReq.build();
        SdkHttpFullRequest signedRequest = signer.sign(sdkRequest, signerParams);

        // Apply signed headers back to Apache request
        req.removeHeaders("Authorization");
        req.removeHeaders("X-Amz-Date");
        req.removeHeaders("X-Amz-Security-Token");

        signedRequest.headers().forEach((name, values) -> {
            if (name.equalsIgnoreCase("Authorization") ||
                name.equalsIgnoreCase("X-Amz-Date") ||
                name.equalsIgnoreCase("X-Amz-Security-Token")) {
                for (String value : values) {
                    req.addHeader(name, value);
                }
            }
        });
    }
}