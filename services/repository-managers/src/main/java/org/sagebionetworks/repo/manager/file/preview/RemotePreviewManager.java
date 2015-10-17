package org.sagebionetworks.repo.manager.file.preview;

import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.PreviewFileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.util.ResourceTracker.ExceedsMaximumResources;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.ServiceUnavailableException;
import org.sagebionetworks.repo.web.TemporarilyUnavailableException;

public interface RemotePreviewManager extends PreviewHandler {

	/**
	 * Get the metadata for a given file.
	 * @param id
	 * @return
	 */
	public FileHandle getFileMetadata(String id) throws NotFoundException;
	
	/**
	 * Generate a preview for the passed file.
	 * @param metadata
	 * @throws Exception 
	 * @throws ServiceUnavailableException 
	 */
	public PreviewFileHandle generatePreview(S3FileHandle metadata) throws Exception;
	

}
