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

import org.sagebionetworks.repo.manager.message.RemoteFilePreviewMessagePublisherImpl;
import org.sagebionetworks.repo.model.file.PreviewFileHandle;
import org.sagebionetworks.repo.model.file.RemoteFilePreviewGenerationRequest;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.util.DefaultClock;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;

public class GenericRemotePreviewGeneratorTest {
	
	AmazonS3Client mockS3Client;
	RemoteFilePreviewMessagePublisherImpl mockMesgPublisher;
	DefaultClock mockClock;
	
	GenericRemotePreviewGenerator generator;

	@Before
	public void setUp() throws Exception {
		mockS3Client = Mockito.mock(AmazonS3Client.class);
		mockMesgPublisher = Mockito.mock(RemoteFilePreviewMessagePublisherImpl.class);
		mockClock = Mockito.mock(DefaultClock.class);
		
		generator = new GenericRemotePreviewGenerator(mockS3Client, mockMesgPublisher, mockClock);
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testClock() {
		when(mockClock.currentTimeMillis()).thenReturn(1000000L, 1400000L, 3L);
		long t = mockClock.currentTimeMillis();
		t = mockClock.currentTimeMillis();
		t = mockClock.currentTimeMillis();
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

		when(mockClock.currentTimeMillis()).thenReturn(1000000L, 1400000L);
		when(mockS3Client.getObjectMetadata(anyString(), anyString())).thenReturn(null);
		
		PreviewFileHandle pfh = generator.generatePreview(inputMetadata);
		verify(mockMesgPublisher).publishToQueue(any(RemoteFilePreviewGenerationRequest.class));
		assertNull(pfh);
	}
	
	@Test
	public void testGeneratePreview() throws InterruptedException, JSONObjectAdapterException, ExecutionException {
		S3FileHandle inputMetadata = new S3FileHandle();
		ObjectMetadata expectedOmd = new ObjectMetadata();

		when(mockClock.currentTimeMillis()).thenReturn(1000000L);
		when(mockS3Client.getObjectMetadata(anyString(), anyString())).thenReturn(expectedOmd);
		
		PreviewFileHandle pfh = generator.generatePreview(inputMetadata);
		verify(mockMesgPublisher).publishToQueue(any(RemoteFilePreviewGenerationRequest.class));
		assertNotNull(pfh);
	}

}
