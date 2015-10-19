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
	LocalPreviewManagerImpl localPreviewManager;
	
	@Autowired
	RemotePreviewManagerImpl remotePreviewManager;
	
	public PreviewManagerImpl(FileHandleDao fileHandleDao, LocalPreviewManagerImpl localPreviewMgr, RemotePreviewManagerImpl remotePreviewMgr) {
		this.fileMetadataDao = fileHandleDao;
		this.localPreviewManager = localPreviewMgr;
		this.remotePreviewManager = remotePreviewMgr;
	}

	@Override
	public boolean canHandleType(ContentType contentType) {
		if (! localPreviewManager.canHandleType(contentType)) {
			return remotePreviewManager.canHandleType(contentType);
		}
		return true;
	}

	@Override
	public void handle(S3FileHandle metadata) throws Exception {
		ContentType cType = ContentType.parse(metadata.getContentType());
		if (localPreviewManager.canHandleType(cType)) {
			localPreviewManager.handle(metadata);
		} else if (remotePreviewManager.canHandleType(cType)) {
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
