package org.sagebionetworks.repo.manager;

import java.io.IOException;
import java.io.UncheckedIOException;

import org.sagebionetworks.StackConfigurationSingleton;
import org.sagebionetworks.aws.v2.S3ObjectContent;
import org.sagebionetworks.aws.v2.S3ObjectStore;
import org.sagebionetworks.downloadtools.FileUtils;
import org.sagebionetworks.utils.ContentTypeUtil;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * A simple utility for reading from the Synapse S3 bucket.
 *
 * @author jmhill
 *
 */
public class AmazonS3UtilityImpl implements AmazonS3Utility{

	private static final String S3_BUCKET = StackConfigurationSingleton.singleton().getS3Bucket();

	@Autowired
	S3ObjectStore objectStore;

	@Override
	public String downloadFromS3ToString(String key) {
		try (S3ObjectContent content = objectStore.getObjectContent(S3_BUCKET, key)) {
			return FileUtils.readStreamAsString(content.getContent(),
					ContentTypeUtil.getCharsetFromContentTypeString(content.getInfo().getContentType()), false);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Override
	public boolean doesExist(String key) {
		try {
			return objectStore.getObjectInfo(S3_BUCKET, key).isPresent();
		} catch (Exception e) {
			// Intentionally broader than "not found": callers fall back to a classpath copy whenever
			// the S3 copy cannot be read, including when S3 itself is unreachable.
			return false;
		}
	}

}
