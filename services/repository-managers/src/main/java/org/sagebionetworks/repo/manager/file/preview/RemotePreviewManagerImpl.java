package org.sagebionetworks.repo.manager.file.preview;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.entity.ContentType;
import org.sagebionetworks.repo.manager.message.RemoteFilePreviewNotificationMessagePublisherImpl;
import org.sagebionetworks.repo.manager.message.RemoteFilePreviewRequestMessagePublisherImpl;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.PreviewFileHandle;
import org.sagebionetworks.repo.model.file.RemoteFilePreviewGenerationRequest;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.util.TempFileProvider;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.TemporarilyUnavailableException;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.S3Object;

public class RemotePreviewManagerImpl implements RemotePreviewManager {

	static private Log log = LogFactory.getLog(RemotePreviewManagerImpl.class);
	
	@Autowired
	FileHandleDao fileMetadataDao;
	
	@Autowired
	AmazonS3Client s3Client;
	
	@Autowired
	TempFileProvider tempFileProvider;
	
	List<PreviewGenerator> generatorList;
	
	/**
	 * Default used by Spring.
	 */
	public RemotePreviewManagerImpl(){}
	


	/**
	 * The Ioc Constructor.
	 * 
	 * @param fileMetadataDao
	 * @param s3Client
	 * @param sqsClient
	 * @param tempFileProvider
	 * @param resourceTracker
	 * @param generatorList
	 * @param maxPreviewMemory
	 */
	public RemotePreviewManagerImpl(FileHandleDao fileMetadataDao,
			AmazonS3Client s3Client,
			TempFileProvider tempFileProvider,
			List<PreviewGenerator> generatorList, Long maxPreviewMemory,
			RemoteFilePreviewRequestMessagePublisherImpl rfprmp,
			RemoteFilePreviewNotificationMessagePublisherImpl rfpnmp) {
		super();
		this.fileMetadataDao = fileMetadataDao;
		this.s3Client = s3Client;
		this.tempFileProvider = tempFileProvider;
		this.generatorList = generatorList;
	}
	
	/**
	 * Injected
	 */
	public void setGeneratorList(List<PreviewGenerator> generatorList){
		this.generatorList = generatorList;
	}
	
	@Override
	public FileHandle getFileMetadata(String id) throws NotFoundException {
		return fileMetadataDao.get(id);
	}

	@Override
	public boolean canHandleType(String contentType, String extension) {
		ContentType ct = ContentType.parse(contentType);
		String mimeType = ct.getMimeType();
		return (this.findPreviewGenerator(mimeType, extension) != null);
	}

	@Override
	public void handle(S3FileHandle metadata) throws Exception {
		this.generatePreview(metadata);
	}

	@Override
	public PreviewFileHandle generatePreview(S3FileHandle metadata)
			throws TemporarilyUnavailableException, Exception {
		
		if(metadata == null) throw new IllegalArgumentException("metadata cannot be null");
		if(metadata.getContentType() == null) throw new IllegalArgumentException("metadata.getContentType() cannot be null");
		if(metadata.getContentSize() == null) throw new IllegalArgumentException("metadata.getContentSize() cannot be null");

		PreviewFileHandle fhRes = null;

		// there is nothing to do if the file is empty
		if (metadata.getContentSize() == 0L) {
			log.info("Cannot generate preview of empty file");
			return null;
		}
		// Try to find a generator for this type
		if (StringUtils.isEmpty(metadata.getContentType())) {
			log.info("Cannot generate preview for file with empty content type");
			return null;
		}
		
		ContentType contentType = ContentType.parse(metadata.getContentType());
		String extension = PreviewGeneratorUtils.findExtension(metadata.getFileName());
		
		final PreviewGenerator gen = findPreviewGenerator(contentType.getMimeType(), extension);
		// there is nothing to do if we do not have a generator for this type
		if(gen == null){
			log.info("No preview generator found for contentType:"+metadata.getContentType());
			return null;
		}
		if (! (gen instanceof RemotePreviewGenerator)) {
			throw new RuntimeException("Generator found is not a remote preview generator.");
		}

		fhRes = generateRemotePreview((RemotePreviewGenerator)gen, metadata);

		return fhRes;
	}
	
	private PreviewFileHandle generateRemotePreview(RemotePreviewGenerator generator, S3FileHandle metadata) throws Exception {
		PreviewFileHandle fp = generator.generatePreview(metadata); 
		return fp;
	}
	
	/**
	 * Find
	 * @param metadta
	 */
	private PreviewGenerator findPreviewGenerator(String mimeType, String extension) {
		mimeType = mimeType.toLowerCase();
		for(PreviewGenerator gen: generatorList) {
			if (gen.supportsContentType(mimeType, extension)) {
				return gen;
			}
		}
		return null;
	}

	public static class S3FilePreviewWatcherThread implements Callable<Boolean> {
		
		private String bucketName;
		private String keyToWatch;
		
		S3FilePreviewWatcherThread(String bucketName, String key) {
			this.bucketName = bucketName;
			this.keyToWatch = key;
		}
		
		@Override
		public Boolean call() {
			return true;
		}
	}

}
