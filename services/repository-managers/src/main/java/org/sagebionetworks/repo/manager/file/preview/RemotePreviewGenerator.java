package org.sagebionetworks.repo.manager.file.preview;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface RemotePreviewGenerator extends PreviewGenerator {

	/**
	 * Generate a preview from the given input, and write it out to the given output stream.
	 * @param from - This stream contains the source data to generate a preview from.
	 * @param to - The preview should be written to this stream.
	 * @return  Must return the content type of generated preview.
	 * @throws IOException 
	 */
	public PreviewOutputMetadata generatePreview(InputStream from, OutputStream to) throws IOException;


}
