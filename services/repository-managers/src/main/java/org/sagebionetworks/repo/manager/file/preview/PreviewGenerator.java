package org.sagebionetworks.repo.manager.file.preview;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Abstraction for generating previews.
 * 
 * @author John
 *
 */
public interface PreviewGenerator {
	
	/**
	 * Return true for each content types supported. Return false for types that are not supported. The content types
	 * are defined by <a href="http://en.wikipedia.org/wiki/Internet_media_type">Internet_media_type</a>
	 * 
	 * @param contentType
	 * @param extension
	 * @return
	 */
	public boolean supportsContentType(String contentType, String extension);
	
	/**
	 * Return true if file preview is generated locally.
	 * 
	 */
	public boolean isLocal();
		
}
