package org.sagebionetworks.repo.manager.file.preview;

import static org.junit.Assert.*;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.sagebionetworks.repo.manager.message.RemoteFilePreviewNotificationMessagePublisherImpl;
import org.sagebionetworks.repo.manager.message.RemoteFilePreviewRequestMessagePublisherImpl;
import org.sagebionetworks.repo.model.file.PreviewFileHandle;
import org.sagebionetworks.repo.model.file.RemoteFilePreviewGenerationRequest;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.util.DefaultClock;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;

public class GenericRemotePreviewGeneratorTest {
	
	RemoteFilePreviewRequestMessagePublisherImpl mockReqMsgPublisher;
	RemoteFilePreviewNotificationMessagePublisherImpl mockNotMsgPublisher;
	
	GenericRemotePreviewGenerator generator;

	@Before
	public void setUp() throws Exception {
		mockReqMsgPublisher = Mockito.mock(RemoteFilePreviewRequestMessagePublisherImpl.class);
		mockNotMsgPublisher = Mockito.mock(RemoteFilePreviewNotificationMessagePublisherImpl.class);
		
		generator = new GenericRemotePreviewGenerator(mockReqMsgPublisher, mockNotMsgPublisher);
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testIsLocal() {
		assertFalse(generator.isLocal());
	}
	
	@Test
	public void testSupports() {
		assertFalse(generator.supportsContentType("abc/def", "ghi"));
		// Until supported
		assertFalse(generator.supportsContentType("text/pdf", "pdf"));
		
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testGeneratePreviewNullS3FileHandle() throws Exception, JSONObjectAdapterException, ExecutionException {
		generator.generatePreview(null);
	}
	
	@Test
	public void testGeneratePreviewReturnNullFph() throws Exception, JSONObjectAdapterException, ExecutionException {
		S3FileHandle inputMetadata = new S3FileHandle();
		ObjectMetadata expectedOmd = new ObjectMetadata();

		PreviewFileHandle pfh = generator.generatePreview(inputMetadata);
		verify(mockReqMsgPublisher).publishToQueue(any(RemoteFilePreviewGenerationRequest.class));
		verify(mockNotMsgPublisher).publishToQueue(any(RemoteFilePreviewGenerationRequest.class));
		assertNull(pfh);
	}
	
}
