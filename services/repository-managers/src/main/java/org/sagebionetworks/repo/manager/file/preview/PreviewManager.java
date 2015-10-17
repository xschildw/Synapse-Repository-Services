package org.sagebionetworks.repo.manager.file.preview;

import org.sagebionetworks.repo.model.file.FileHandle;

public interface PreviewManager extends PreviewHandler {
	
	public FileHandle getFileMetadata(String id);

}
