package org.sagebionetworks.repo.manager.file.preview;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.io.File;
import java.io.FileOutputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.repo.manager.message.RemoteFilePreviewNotificationMessagePublisherImpl;
import org.sagebionetworks.repo.manager.message.RemoteFilePreviewRequestMessagePublisherImpl;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.file.PreviewFileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.util.TempFileProvider;
import org.sagebionetworks.repo.web.TemporarilyUnavailableException;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.sns.AmazonSNSClient;

public class RemotePreviewManagerImplTest {
	
	RemotePreviewManagerImpl previewManager;
	FileHandleDao stubFileMetadataDao;
	
	@Mock
	private AmazonSNSClient mockSNSClient;
	@Mock
	private TempFileProvider mockFileProvider;
	@Mock
	private RemotePreviewGenerator mockRemotePreviewGenerator;
	@Mock
	private File mockUploadFile;
	@Mock
	private S3Object mockS3Object;
	@Mock
	private FileOutputStream mockOutputStream;
	@Mock
	private S3ObjectInputStream mockS3ObjectInputStream;
	@Mock
	private RemoteFilePreviewRequestMessagePublisherImpl mockRemoteFilePreviewRequestMessagePublisher;
	@Mock
	private RemoteFilePreviewNotificationMessagePublisherImpl mockRemoteFilePreviewNotificationMessagePublisher;
	
	Long maxPreviewSize = 100l;
	float multiplerForContentType = 1.5f;
	String testValidLocalContentType = "text/plain";
	String testValidRemoteContentType = "application/msword";
	PreviewOutputMetadata previewContentType = new PreviewOutputMetadata("application/zip", ".zip");
	S3FileHandle testLocalMetadata;
	S3FileHandle testRemoteMetadata;
	Long resultPreviewSize = 15l;
	

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		stubFileMetadataDao = new StubFileMetadataDao();
		
		List<PreviewGenerator> genList = new LinkedList<PreviewGenerator>();
		genList.add(mockRemotePreviewGenerator);

		// This is a remote test file metadata
		testRemoteMetadata = new S3FileHandle();
		testRemoteMetadata.setBucketName("bucketName");
		testRemoteMetadata.setContentType(testValidRemoteContentType);
		testRemoteMetadata.setContentMd5("contentMD5");
		testRemoteMetadata.setContentSize(10l);
		testRemoteMetadata.setCreatedBy("createdBy");
		testRemoteMetadata.setEtag("etag");
		testRemoteMetadata.setFileName("rfileName.doc");
		testRemoteMetadata.setKey("key");
		// Add this to the stub
		testRemoteMetadata = stubFileMetadataDao.createFile(testRemoteMetadata);
		
		when(mockRemotePreviewGenerator.isLocal()).thenReturn(false);
		when(mockRemotePreviewGenerator.supportsContentType(testValidLocalContentType, "txt")).thenReturn(false);
		when(mockRemotePreviewGenerator.supportsContentType(testValidRemoteContentType, "doc")).thenReturn(true);
		when(mockRemotePreviewGenerator.generatePreview(testRemoteMetadata)).thenReturn(null);

		previewManager = new RemotePreviewManagerImpl(stubFileMetadataDao, mockFileProvider, genList, maxPreviewSize, mockRemoteFilePreviewRequestMessagePublisher, mockRemoteFilePreviewNotificationMessagePublisher);

	}

	@After
	public void tearDown() throws Exception {
	}

	
	@Test(expected=IllegalArgumentException.class)
	public void testNullMetadata() throws Exception {
		previewManager.generatePreview(null);
	}
	
	@Test
	public void testBadContent() throws Exception {
		S3FileHandle bad = new S3FileHandle();
		try {
			previewManager.generatePreview(bad);
		} catch (IllegalArgumentException e) {
			// Expected
		}
		bad.setContentType("contentType");
		try {
			previewManager.generatePreview(bad);
		} catch (IllegalArgumentException e) {
			// Expected
		}
		//	Should not throw any exception here...
	}
	
	@Test
	public void testZeroContentSize() throws Exception {
		testRemoteMetadata.setContentSize(0L);
		PreviewFileHandle pfh = previewManager.generatePreview(testRemoteMetadata);
		assertNull(pfh);
		verify(mockRemotePreviewGenerator, never()).generatePreview(any(S3FileHandle.class));
		
	}
	
	@Test
	public void testEmptyContentType() throws Exception {
		testRemoteMetadata.setContentType("");
		PreviewFileHandle pfh = previewManager.generatePreview(testRemoteMetadata);
		assertNull(pfh);
		verify(mockRemotePreviewGenerator, never()).generatePreview(any(S3FileHandle.class));
	}
	
	@Test
	public void testInvalidContentType() throws Exception {
		testRemoteMetadata.setContentType("invalid");
		PreviewFileHandle pfh = previewManager.generatePreview(testRemoteMetadata);
		assertNull(pfh);
		verify(mockRemotePreviewGenerator, never()).generatePreview(any(S3FileHandle.class));
	}
	
	
	// Just check basic wiring
	@Test
	public void testExpectedRemotePreview() throws Exception {
		PreviewFileHandle pfm = previewManager.generatePreview(testRemoteMetadata);
		assertNull(pfm);
		verify(mockRemotePreviewGenerator).generatePreview(any(S3FileHandle.class));
	}

}
