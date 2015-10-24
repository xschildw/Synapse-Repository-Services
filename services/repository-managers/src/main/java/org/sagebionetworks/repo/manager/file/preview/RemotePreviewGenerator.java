package org.sagebionetworks.repo.manager.file.preview;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutionException;

import org.sagebionetworks.repo.model.file.PreviewFileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

public interface RemotePreviewGenerator extends PreviewGenerator {

	/**
	 * Generate a preview from the given input, and write it out to the given output stream.
	 * @param from - This stream contains the source data to generate a preview from.
	 * @param to - The preview should be written to this stream.
	 * @return  Must return the content type of generated preview.
	 * @throws IOException 
	 */
	public PreviewFileHandle generatePreview(S3FileHandle inputMetadata) throws InterruptedException, JSONObjectAdapterException, ExecutionException;


}
