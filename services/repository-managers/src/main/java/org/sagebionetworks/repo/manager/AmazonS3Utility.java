package org.sagebionetworks.repo.manager;

/**
 * This is a utility that reads files from the Synapse S3 bucket.
 * @author jmhill
 *
 */
public interface AmazonS3Utility {

	/**
	 * Does the given key exist?
	 * @param key
	 * @return False when the object is missing or cannot be reached for any reason
	 */
	public boolean doesExist(String key);

	/**
	 * Download an S3 file into a String, decoding using the charset in the content-type header,
	 * or UTF-8 when the content type names no charset.
	 * Note:  It's the callers responsibility to ensure the content is small enough
	 * to fit easily in the machine's memory.
	 * @param key
	 * @return
	 */
	String downloadFromS3ToString(String key);

}
