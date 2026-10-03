package org.sagebionetworks.aws.v2;

import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.http.HttpStatusCode;
import software.amazon.awssdk.services.s3.model.S3Exception;

/**
 * Shared classification of AWS SDK v2 service failures, so that callers of {@link S3ObjectStore}
 * and of a raw {@link software.amazon.awssdk.services.s3.S3Client} make the same retry, not-found
 * and bucket-access decisions.
 * <p>
 * Every predicate is null-safe and answers false (or null) for anything that is not an
 * {@link AwsServiceException}. In particular a client-side failure such as a network timeout is
 * never a service failure.
 */
public final class S3Errors {

	private static final String NO_SUCH_BUCKET = "NoSuchBucket";
	private static final String PERMANENT_REDIRECT = "PermanentRedirect";

	private S3Errors() {
	}

	/**
	 * @return true if the failure is a service-side error (HTTP status 500 or above) that is worth
	 *         retrying later.
	 */
	public static boolean isServiceFailure(Throwable e) {
		// Mirrors the v1 ErrorType.Service classification. SdkException.retryable() is deliberately
		// not used: it also covers throttling and client-side failures that were never retried.
		return e instanceof AwsServiceException && ((AwsServiceException) e).statusCode() >= 500;
	}

	/**
	 * @return true if the failure is an HTTP 404.
	 */
	public static boolean isNotFound(Throwable e) {
		// Matched on status rather than error code: HEAD responses have no body, so they carry no
		// error code.
		return e instanceof AwsServiceException
				&& ((AwsServiceException) e).statusCode() == HttpStatusCode.NOT_FOUND;
	}

	/**
	 * @return the service error code (e.g. NoSuchKey, AccessDenied), or null if the failure is not
	 *         a service exception or carries no error code.
	 */
	public static String errorCode(Throwable e) {
		if (!(e instanceof AwsServiceException)) {
			return null;
		}
		AwsErrorDetails details = ((AwsServiceException) e).awsErrorDetails();
		return details == null ? null : details.errorCode();
	}

	/**
	 * Distinguishes "Synapse cannot reach this bucket" from a failure scoped to the object.
	 *
	 * @return true for an error code of NoSuchBucket or PermanentRedirect, or any 403.
	 */
	public static boolean isBucketAccessFailure(S3Exception e) {
		// A cross-region client only learns a bucket's region reactively, so an unreachable bucket
		// has no single failure mode: it surfaces as NoSuchBucket, as a PermanentRedirect the SDK
		// could not follow, or as a 403 from whichever operation was attempted.
		if (e == null) {
			return false;
		}
		if (HttpStatusCode.FORBIDDEN == e.statusCode()) {
			return true;
		}
		String errorCode = errorCode(e);
		return NO_SUCH_BUCKET.equals(errorCode) || PERMANENT_REDIRECT.equals(errorCode);
	}

}
