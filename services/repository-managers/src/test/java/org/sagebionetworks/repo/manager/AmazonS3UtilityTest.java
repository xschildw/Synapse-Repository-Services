package org.sagebionetworks.repo.manager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.aws.v2.S3ObjectStore;
import org.sagebionetworks.aws.v2.S3WriteOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class AmazonS3UtilityTest {

	private static final String TEST_KEY = "org_sagebionetworks_repo_manager_AmazonS3UtilityTest";

	@Autowired
	private AmazonS3Utility s3Utility;

	@Autowired
	private S3ObjectStore objectStore;

	@Autowired
	private StackConfiguration stackConfiguration;

	private String bucket;

	@BeforeEach
	public void before() {
		bucket = stackConfiguration.getS3Bucket();
		objectStore.deleteObject(bucket, TEST_KEY);
	}

	@AfterEach
	public void after() {
		objectStore.deleteObject(bucket, TEST_KEY);
	}

	@Test
	public void testDownloadFromS3ToStringWithContentTypeCharset() {
		// Non-ASCII content in a non-default charset only round trips if the charset is read from
		// the content type.
		String content = "Ça coûte très cher à Zürich.";
		putObject(content, StandardCharsets.ISO_8859_1, "text/plain; charset=ISO-8859-1");

		// call under test
		String retrieved = s3Utility.downloadFromS3ToString(TEST_KEY);

		assertEquals(content, retrieved);
	}

	@Test
	public void testDownloadFromS3ToStringWithMalformedContentType() {
		// Objects written by an earlier version of this utility carry this malformed content type.
		String content = "Ça coûte très cher à Zürich.";
		putObject(content, StandardCharsets.UTF_8, "text/plain);charset=utf-8");

		// call under test
		String retrieved = s3Utility.downloadFromS3ToString(TEST_KEY);

		assertEquals(content, retrieved);
	}

	@Test
	public void testDownloadFromS3ToStringWithoutCharset() {
		String content = "Ça coûte très cher à Zürich.";
		putObject(content, StandardCharsets.UTF_8, "text/plain");

		// call under test
		String retrieved = s3Utility.downloadFromS3ToString(TEST_KEY);

		assertEquals(content, retrieved);
	}

	@Test
	public void testDoesExist() {
		putObject("some content", StandardCharsets.UTF_8, "text/plain");

		// call under test
		assertTrue(s3Utility.doesExist(TEST_KEY));
	}

	@Test
	public void testDoesExistWithMissingKey() {
		// call under test
		assertFalse(s3Utility.doesExist(TEST_KEY));
	}

	private void putObject(String content, Charset charset, String contentType) {
		byte[] bytes = content.getBytes(charset);
		objectStore.putObject(bucket, TEST_KEY, new ByteArrayInputStream(bytes), bytes.length,
				S3WriteOptions.builder().withContentType(contentType).build());
	}

}
