package org.sagebionetworks.repo.manager.file.preview;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.repo.manager.message.RemoteFilePreviewMessagePublisher;
import org.sagebionetworks.repo.manager.message.RemoteFilePreviewMessagePublisherImpl;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.file.PreviewFileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.util.ResourceTracker;
import org.sagebionetworks.repo.util.ResourceTracker.ExceedsMaximumResources;
import org.sagebionetworks.repo.util.TempFileProvider;
import org.sagebionetworks.repo.web.TemporarilyUnavailableException;
import org.sagebionetworks.workers.util.aws.message.MessageQueueConfiguration;
import org.sagebionetworks.workers.util.aws.message.MessageQueueImpl;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sqs.AmazonSQSClient;

public class PreviewManagerImplTest {
	
	PreviewManagerImpl previewManager;
	FileHandleDao stubFileMetadataDao;
	@Mock
	private AmazonS3Client mockS3Client;
	@Mock
	private AmazonSNSClient mockSNSClient;
	@Mock
	private TempFileProvider mockFileProvider;
	@Mock
	private LocalPreviewGenerator mockLocalPreviewGenerator;
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
	private RemoteFilePreviewMessagePublisherImpl mockRemoteFilePreviewMessagePublisher;
	
	ExecutorService executorSvc = Executors.newSingleThreadExecutor();
	
	Long maxPreviewSize = 100l;
	float multiplerForContentType = 1.5f;
	String testValidLocalContentType = "text/plain";
	String testValidRemoteContentType = "application/msword";
	PreviewOutputMetadata previewContentType = new PreviewOutputMetadata("application/zip", ".zip");
	S3FileHandle testLocalMetadata;
	S3FileHandle testRemoteMetadata;
	Long resultPreviewSize = 15l;
	
	@Before
	public void before() throws IOException{
		MockitoAnnotations.initMocks(this);
		stubFileMetadataDao = new StubFileMetadataDao();
		when(mockFileProvider.createTempFile(any(String.class), any(String.class))).thenReturn(mockUploadFile);
		when(mockFileProvider.createFileOutputStream(mockUploadFile)).thenReturn(mockOutputStream);
		when(mockS3Client.getObject(any(GetObjectRequest.class))).thenReturn(mockS3Object);
		when(mockS3Object.getObjectContent()).thenReturn(mockS3ObjectInputStream);
		when(mockLocalPreviewGenerator.isLocal()).thenReturn(true);
		when(mockLocalPreviewGenerator.supportsContentType(testValidLocalContentType, "txt")).thenReturn(true);
		when(mockLocalPreviewGenerator.supportsContentType(testValidRemoteContentType, "doc")).thenReturn(false);
		when(mockLocalPreviewGenerator.calculateNeededMemoryBytesForPreview(testValidLocalContentType, maxPreviewSize + 1)).thenReturn(maxPreviewSize + 1);
		when(mockLocalPreviewGenerator.generatePreview(mockS3ObjectInputStream, mockOutputStream)).thenReturn(previewContentType);
		when(mockUploadFile.length()).thenReturn(resultPreviewSize);
		when(mockRemotePreviewGenerator.isLocal()).thenReturn(false);
		when(mockRemotePreviewGenerator.supportsContentType(testValidLocalContentType, "txt")).thenReturn(false);
		when(mockRemotePreviewGenerator.supportsContentType(testValidRemoteContentType, "doc")).thenReturn(true);
		when(mockRemotePreviewGenerator.generatePreview(mockS3ObjectInputStream, mockOutputStream)).thenReturn(previewContentType);
		when(mockUploadFile.length()).thenReturn(resultPreviewSize);
		List<PreviewGenerator> genList = new LinkedList<PreviewGenerator>();
		genList.add(mockLocalPreviewGenerator);
		genList.add(mockRemotePreviewGenerator);
		
		previewManager = new PreviewManagerImpl(stubFileMetadataDao, mockS3Client, mockFileProvider, genList, maxPreviewSize, mockRemoteFilePreviewMessagePublisher, executorSvc);
		
		// This is a local test file metadata
		testLocalMetadata = new S3FileHandle();
		testLocalMetadata.setBucketName("bucketName");
		testLocalMetadata.setContentType(testValidLocalContentType);
		testLocalMetadata.setContentMd5("contentMD5");
		testLocalMetadata.setContentSize(10l);
		testLocalMetadata.setCreatedBy("createdBy");
		testLocalMetadata.setEtag("etag");
		testLocalMetadata.setFileName("lfileName.txt");
		testLocalMetadata.setKey("key");
		// Add this to the stub
		testLocalMetadata = stubFileMetadataDao.createFile(testLocalMetadata);

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
}
	
	@Test (expected=IllegalArgumentException.class)
	public void testMetadataNull() throws Exception{
		previewManager.generatePreview(null);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testContentTypeNullNull() throws Exception{
		testLocalMetadata.setContentType(null);
		previewManager.generatePreview(testLocalMetadata);
	}
	
	@Test
	public void testContentTypeEmpty() throws Exception {
		testLocalMetadata.setContentType("");
		PreviewFileHandle pfm = previewManager.generatePreview(testLocalMetadata);
		assertNull(pfm);
	}

	@Test (expected=IllegalArgumentException.class)
	public void testContentSizelNull() throws Exception{
		testLocalMetadata.setContentSize(null);
		previewManager.generatePreview(testLocalMetadata);
	}
	
	@Test
	public void testUnsupportedType() throws Exception{
		// Set to an unsupported content type;
		testLocalMetadata.setContentType("fake/type");
		PreviewFileHandle pfm = previewManager.generatePreview(testLocalMetadata);
		assertTrue(pfm == null);
	}
	
	@Test
	public void testContentSizeTooLarge() throws Exception{
		// set the file size to be one byte too large.
		long size = maxPreviewSize + 1;
		testLocalMetadata.setContentSize(size);
		PreviewFileHandle pfm = previewManager.generatePreview(testLocalMetadata);
		assertNull(pfm);
	}

	@Test
	public void testContentSizeNoTooLarge() throws Exception {
		// set the file size to be one byte too large.
		long size = maxPreviewSize;
		testLocalMetadata.setContentSize(size);
		PreviewFileHandle pfm = previewManager.generatePreview(testLocalMetadata);
		assertNotNull(pfm);
	}

	@Test (expected=TemporarilyUnavailableException.class)
	public void testTemporarilyUnavailable() throws Exception{
		// Simulate a TemporarilyUnavailable exception.
		previewManager.resourceTracker = Mockito.mock(ResourceTracker.class);
		when(previewManager.resourceTracker.allocateAndUseResources(any(Callable.class), any(Long.class))).thenThrow(new TemporarilyUnavailableException());
		PreviewFileHandle pfm = previewManager.generatePreview(testLocalMetadata);
		assertTrue(pfm == null);
	}

	@Test
	public void testExceedsMaximumResources() throws Exception{
		// Simulate a ExceedsMaximumResources exception.
		previewManager.resourceTracker = Mockito.mock(ResourceTracker.class);
		when(previewManager.resourceTracker.allocateAndUseResources(any(Callable.class), any(Long.class))).thenThrow(new ExceedsMaximumResources());
		PreviewFileHandle pfm = previewManager.generatePreview(testLocalMetadata);
		assertTrue(pfm == null);
	}
	
	@Test
	public void testStreamsClosed() throws Exception{
		// Simulate an S3 exception.  The streams must be closed even when there is an error
		when(mockS3Client.putObject(any(PutObjectRequest.class))).thenThrow(new RuntimeException("Something went wrong!"));
		try{
			previewManager.generatePreview(testLocalMetadata);
			fail("RuntimeException should have been thrown");
		}catch(RuntimeException e){
			// expected
		}
		// Validate the streams were closed
		verify(mockOutputStream, atLeast(1)).close();
		verify(mockS3ObjectInputStream, atLeast(1)).abort();
	}

	@Test
	public void testTempFilesDeleted() throws Exception{
		// Simulate an S3 exception.  The temp files must be deleted.
		when(mockS3Client.putObject(any(PutObjectRequest.class))).thenThrow(new RuntimeException("Something went wrong!"));
		try{
			PreviewFileHandle pfm = previewManager.generatePreview(testLocalMetadata);
			fail("RuntimeException should have been thrown");
		}catch(RuntimeException e){
			// expected
		}
		// Validate the temp files were deleted
		verify(mockUploadFile, atLeast(1)).delete();
	}
	
	@Test
	public void testExpectedLocalPreview() throws Exception{
		PreviewFileHandle pfm = previewManager.generatePreview(testLocalMetadata);
		assertNotNull(pfm);
		assertNotNull(pfm.getId());
		assertEquals(previewContentType.getContentType(), pfm.getContentType());
		assertEquals(testLocalMetadata.getCreatedBy(), pfm.getCreatedBy());
		assertNotNull(pfm.getCreatedOn());
		assertEquals("preview"+previewContentType.getExtension(), pfm.getFileName());
		assertEquals(resultPreviewSize, pfm.getContentSize());
		// Make sure the preview is in the dao
		PreviewFileHandle fromDao = (PreviewFileHandle) stubFileMetadataDao.get(pfm.getId());
		assertEquals(pfm, fromDao);
	}
	
	// Just check basic wiring
	@Test
	public void testExpectedRemotePreview() throws Exception {
		when(mockS3Client.getObject(any(String.class), any(String.class))).thenReturn(this.expectedS3Object(7000));
		PreviewFileHandle pfm = previewManager.generatePreview(testRemoteMetadata);
		assertNotNull(pfm);
	}
	
	private S3Object expectedS3Object(long delayMS) throws InterruptedException {
		S3Object expectedS3Object = new S3Object();
		expectedS3Object.setBucketName(testRemoteMetadata.getBucketName());
		expectedS3Object.setKey(testRemoteMetadata.getKey());
		Thread.sleep(delayMS);
		return expectedS3Object;
	}
}
