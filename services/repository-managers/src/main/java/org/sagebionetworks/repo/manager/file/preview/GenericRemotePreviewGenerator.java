package org.sagebionetworks.repo.manager.file.preview;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.manager.file.preview.RemotePreviewManagerImpl.S3FilePreviewWatcherThread;
import org.sagebionetworks.repo.manager.message.RemoteFilePreviewNotificationMessagePublisherImpl;
import org.sagebionetworks.repo.manager.message.RemoteFilePreviewRequestMessagePublisherImpl;
import org.sagebionetworks.repo.model.file.PreviewFileHandle;
import org.sagebionetworks.repo.model.file.RemoteFilePreviewGenerationRequest;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.util.Clock;
import org.sagebionetworks.util.DefaultClock;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.google.common.collect.ImmutableSet;

public class GenericRemotePreviewGenerator implements RemotePreviewGenerator {

	private static Log log = LogFactory.getLog(GenericRemotePreviewGenerator.class);

	public static final Set<String> REMOTE_MIME_TYPES = ImmutableSet
			.<String> builder()
			.add("application/vnd.openxmlformats-officedocument.wordprocessingml.document",
					"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "application/vnd.ms-excel", "text/rtf",
					"application/rtf", "application/vnd.openxmlformats-officedocument.presentationml.presentation",
					"application/vnd.ms-powerpoint", "application/x-mspublisher", "application/xls", "application/postscript",
					"application/msword", "application/vnd.oasis.opendocument.spreadsheet", "application/msexcel", "application/excel",
					"application/vnd.openxmlformats-officedoc", "application/vnd.openxmlformats-officedocument.word",
					"application/vnd.oasis.opendocument.text", "application/vnd.ms-word.document.macroEnabled.12", "application/powerpoint",
					"text/pdf", "text/x-pdf", "application/pdf")
			.build();

	@Autowired
	AmazonS3Client s3Client;
	
	@Autowired
	RemoteFilePreviewRequestMessagePublisherImpl remoteFilePreviewRequestMessagePublisher;
	
	@Autowired
	RemoteFilePreviewNotificationMessagePublisherImpl remoteFilePreviewNotificationMessagePublisher;
	
	/* Used by Spring */
	public GenericRemotePreviewGenerator() {
		
	}
	
	public GenericRemotePreviewGenerator(AmazonS3Client client, RemoteFilePreviewRequestMessagePublisherImpl rfpReqPublisher, RemoteFilePreviewNotificationMessagePublisherImpl rfpNotPublisher) {
		this.s3Client = client;
		this.remoteFilePreviewRequestMessagePublisher = rfpReqPublisher;
		this.remoteFilePreviewNotificationMessagePublisher = rfpNotPublisher;
	}
	
	@Override
	public boolean supportsContentType(String mimeType, String extension) {
		if(!StackConfiguration.singleton().getRemoteFilePreviewsEnabled()){
			return false;
		}
		return REMOTE_MIME_TYPES.contains(mimeType);
	}

	@Override
	public boolean isLocal() {
		return false;
	}

	public PreviewFileHandle generatePreview(S3FileHandle inputMetadata) throws InterruptedException, JSONObjectAdapterException, ExecutionException {
		
		if (inputMetadata == null) {
			throw new IllegalArgumentException("InputMetadata cannot be null.");
		}
		
		// Send the request
		S3FileHandle out = new S3FileHandle();
		out.setBucketName(inputMetadata.getBucketName());
		out.setFileName("preview.png");
		out.setKey(inputMetadata.getCreatedBy() + UUID.randomUUID().toString());
		RemoteFilePreviewGenerationRequest req = PreviewGeneratorUtils.createRemoteFilePreviewGenerationRequest(inputMetadata, out);
		remoteFilePreviewRequestMessagePublisher.publishToQueue(req);
		remoteFilePreviewNotificationMessagePublisher.publishToQueue(req);

		// Creating the actual file handle is done by a worker looking at the remote preview notifications queue
		return null;
	}

}
