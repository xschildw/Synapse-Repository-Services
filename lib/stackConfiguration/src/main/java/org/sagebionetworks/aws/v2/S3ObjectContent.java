package org.sagebionetworks.aws.v2;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

import software.amazon.awssdk.http.Abortable;

/**
 * The content of an object opened for reading, together with the metadata S3 returned on the same
 * response. The caller owns the content and must either {@link #close()} or {@link #abort()} it.
 */
public class S3ObjectContent implements Closeable {

	private final S3ObjectInfo info;
	private final InputStream content;

	public S3ObjectContent(S3ObjectInfo info, InputStream content) {
		if (info == null) {
			throw new IllegalArgumentException("info is required.");
		}
		if (content == null) {
			throw new IllegalArgumentException("content is required.");
		}
		this.info = info;
		this.content = content;
	}

	public S3ObjectInfo getInfo() {
		return info;
	}

	public InputStream getContent() {
		return content;
	}

	/**
	 * Releases the content without reading what remains of it. Use this instead of {@link #close()}
	 * when the content may have been only partially read, since closing drains the rest of the
	 * object over the network.
	 */
	public void abort() {
		if (content instanceof Abortable) {
			((Abortable) content).abort();
			return;
		}
		try {
			content.close();
		} catch (IOException e) {
			// Abort is a best-effort release; there is nothing left to read the content for.
		}
	}

	@Override
	public void close() throws IOException {
		content.close();
	}

}
