package org.sagebionetworks.aws.v2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.http.HttpStatusCode;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;

public class S3ErrorsTest {

	@Test
	public void testIsServiceFailureWithServerError() {
		// call under test
		assertTrue(S3Errors.isServiceFailure(s3Exception(HttpStatusCode.INTERNAL_SERVER_ERROR, "InternalError")));
	}

	@Test
	public void testIsServiceFailureWithServiceUnavailable() {
		// call under test
		assertTrue(S3Errors.isServiceFailure(s3Exception(HttpStatusCode.SERVICE_UNAVAILABLE, "SlowDown")));
	}

	@Test
	public void testIsServiceFailureWithNonS3ServiceException() {
		AwsServiceException e = AwsServiceException.builder().statusCode(HttpStatusCode.SERVICE_UNAVAILABLE).build();

		// call under test
		assertTrue(S3Errors.isServiceFailure(e));
	}

	@Test
	public void testIsServiceFailureWithClientError() {
		// call under test
		assertFalse(S3Errors.isServiceFailure(s3Exception(HttpStatusCode.BAD_REQUEST, "InvalidRequest")));
	}

	@Test
	public void testIsServiceFailureWithSdkClientException() {
		// Network and timeout failures were never retried under v1 either.
		SdkClientException e = SdkClientException.builder().message("timeout").build();

		// call under test
		assertFalse(S3Errors.isServiceFailure(e));
	}

	@Test
	public void testIsServiceFailureWithNull() {
		// call under test
		assertFalse(S3Errors.isServiceFailure(null));
	}

	@Test
	public void testIsNotFoundWithNoSuchKey() {
		// call under test
		assertTrue(S3Errors.isNotFound(s3Exception(HttpStatusCode.NOT_FOUND, "NoSuchKey")));
	}

	@Test
	public void testIsNotFoundWithModeledException() {
		NoSuchKeyException e = NoSuchKeyException.builder().statusCode(HttpStatusCode.NOT_FOUND).build();

		// call under test
		assertTrue(S3Errors.isNotFound(e));
	}

	@Test
	public void testIsNotFoundWithoutErrorCode() {
		// A HEAD response has no body, so the 404 carries no error code.
		S3Exception e = (S3Exception) S3Exception.builder().statusCode(HttpStatusCode.NOT_FOUND).build();

		// call under test
		assertTrue(S3Errors.isNotFound(e));
	}

	@Test
	public void testIsNotFoundWithForbidden() {
		// call under test
		assertFalse(S3Errors.isNotFound(s3Exception(HttpStatusCode.FORBIDDEN, "AccessDenied")));
	}

	@Test
	public void testIsNotFoundWithNonServiceException() {
		// call under test
		assertFalse(S3Errors.isNotFound(new IllegalStateException("not an AWS failure")));
	}

	@Test
	public void testIsNotFoundWithNull() {
		// call under test
		assertFalse(S3Errors.isNotFound(null));
	}

	@Test
	public void testErrorCode() {
		// call under test
		assertEquals("NoSuchKey", S3Errors.errorCode(s3Exception(HttpStatusCode.NOT_FOUND, "NoSuchKey")));
	}

	@Test
	public void testErrorCodeWithoutErrorDetails() {
		S3Exception e = (S3Exception) S3Exception.builder().statusCode(HttpStatusCode.NOT_FOUND).build();

		// call under test
		assertNull(S3Errors.errorCode(e));
	}

	@Test
	public void testErrorCodeWithNonServiceException() {
		// call under test
		assertNull(S3Errors.errorCode(new IllegalStateException("not an AWS failure")));
	}

	@Test
	public void testErrorCodeWithNull() {
		// call under test
		assertNull(S3Errors.errorCode(null));
	}

	@Test
	public void testIsBucketAccessFailureWithNoSuchBucket() {
		// call under test
		assertTrue(S3Errors.isBucketAccessFailure(s3Exception(HttpStatusCode.NOT_FOUND, "NoSuchBucket")));
	}

	@Test
	public void testIsBucketAccessFailureWithPermanentRedirect() {
		// call under test
		assertTrue(S3Errors.isBucketAccessFailure(s3Exception(HttpStatusCode.MOVED_PERMANENTLY, "PermanentRedirect")));
	}

	@Test
	public void testIsBucketAccessFailureWithForbidden() {
		// call under test
		assertTrue(S3Errors.isBucketAccessFailure(s3Exception(HttpStatusCode.FORBIDDEN, "AccessDenied")));
	}

	@Test
	public void testIsBucketAccessFailureWithForbiddenWithoutErrorCode() {
		S3Exception e = (S3Exception) S3Exception.builder().statusCode(HttpStatusCode.FORBIDDEN).build();

		// call under test
		assertTrue(S3Errors.isBucketAccessFailure(e));
	}

	@Test
	public void testIsBucketAccessFailureWithNoSuchKey() {
		// A failure scoped to the object is not a bucket failure.
		// call under test
		assertFalse(S3Errors.isBucketAccessFailure(s3Exception(HttpStatusCode.NOT_FOUND, "NoSuchKey")));
	}

	@Test
	public void testIsBucketAccessFailureWithServerError() {
		// call under test
		assertFalse(S3Errors.isBucketAccessFailure(s3Exception(HttpStatusCode.INTERNAL_SERVER_ERROR, "InternalError")));
	}

	@Test
	public void testIsBucketAccessFailureWithNull() {
		// call under test
		assertFalse(S3Errors.isBucketAccessFailure(null));
	}

	private static S3Exception s3Exception(int statusCode, String errorCode) {
		return (S3Exception) S3Exception.builder()
				.statusCode(statusCode)
				.awsErrorDetails(AwsErrorDetails.builder().errorCode(errorCode).build())
				.build();
	}

}
