package org.sagebionetworks.repo.manager.file.preview;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

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
import org.sagebionetworks.repo.manager.message.RemoteFilePreviewMessagePublisherImpl;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.file.PreviewFileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.util.TempFileProvider;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.sns.AmazonSNSClient;

public class RemotePreviewManagerImplTest {
	
	RemotePreviewManagerImpl previewManager;
	FileHandleDao stubFileMetadataDao;
	
	@Mock
	private AmazonS3Client mockS3Client;
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
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		stubFileMetadataDao = new StubFileMetadataDao();
		
		when(mockRemotePreviewGenerator.isLocal()).thenReturn(false);
		when(mockRemotePreviewGenerator.supportsContentType(testValidLocalContentType, "txt")).thenReturn(false);
		when(mockRemotePreviewGenerator.supportsContentType(testValidRemoteContentType, "doc")).thenReturn(true);
		when(mockRemotePreviewGenerator.generatePreview(mockS3ObjectInputStream, mockOutputStream)).thenReturn(previewContentType);

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

		previewManager = new RemotePreviewManagerImpl(stubFileMetadataDao, mockS3Client, mockFileProvider, genList, maxPreviewSize, mockRemoteFilePreviewMessagePublisher, executorSvc);

	}

	@After
	public void tearDown() throws Exception {
	}

	// Just check basic wiring
	@Test
	public void testExpectedRemotePreview() throws Exception {
		when(mockS3Client.getObject(any(String.class), any(String.class))).thenReturn(this.expectedS3Object(30000));
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
