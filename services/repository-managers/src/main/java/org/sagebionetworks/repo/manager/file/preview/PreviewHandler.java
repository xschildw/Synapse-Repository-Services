package org.sagebionetworks.repo.manager.file.preview;

import org.apache.http.entity.ContentType;
import org.sagebionetworks.repo.model.file.S3FileHandle;

public interface PreviewHandler {
	
	boolean canHandleType(String contentType, String extension);
	
	void handle(S3FileHandle metadata) throws Exception;
}
