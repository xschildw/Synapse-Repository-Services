package org.sagebionetworks.repo.manager.file.preview;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.entity.ContentType;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.springframework.beans.factory.annotation.Autowired;

public class PreviewManagerImpl implements PreviewManager {

	static private Log log = LogFactory.getLog(PreviewManagerImpl.class);

	@Autowired
	FileHandleDao fileMetadataDao;
	
	@Autowired
	LocalPreviewManager localPreviewManager;
	
	@Autowired
	RemotePreviewManager remotePreviewManager;
	
	/* Used by Spring */
	public PreviewManagerImpl() {
		
	};
	
	public PreviewManagerImpl(FileHandleDao fileHandleDao, LocalPreviewManager localPreviewMgr, RemotePreviewManager remotePreviewMgr) {
		this.fileMetadataDao = fileHandleDao;
		this.localPreviewManager = localPreviewMgr;
		this.remotePreviewManager = remotePreviewMgr;
	}

	@Override
	public boolean canHandleType(String contentType, String extension) {
		if (! localPreviewManager.canHandleType(contentType, extension)) {
			return remotePreviewManager.canHandleType(contentType, extension);
		}
		return true;
	}

	@Override
	public void handle(S3FileHandle metadata) throws Exception {
		String cType = metadata.getContentType();
		String ext = PreviewGeneratorUtils.findExtension(metadata.getFileName());
		if (localPreviewManager.canHandleType(cType, ext)) {
			localPreviewManager.handle(metadata);
		} else if (remotePreviewManager.canHandleType(cType, ext)) {
			remotePreviewManager.handle(metadata);
		} else {
			throw new RuntimeException("Content type cannot be handled!");
		}

	}

	@Override
	public FileHandle getFileMetadata(String id) {
		return fileMetadataDao.get(id);
	}

}
