package org.sagebionetworks.repo.manager.file.preview;

import static org.junit.Assert.*;

import org.apache.http.entity.ContentType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.mockito.Mockito.when;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Matchers.anyString;

import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;

public class PreviewManagerImplTest {
	
	FileHandleDao mockFileHandleDao;
	LocalPreviewManagerImpl mockLocalPreviewMgr;
	RemotePreviewManagerImpl mockRemotePreviewMgr;
	PreviewManagerImpl previewMgr;

	@Before
	public void setUp() throws Exception {
		mockFileHandleDao = Mockito.mock(FileHandleDao.class);
		mockLocalPreviewMgr = Mockito.mock(LocalPreviewManagerImpl.class);
		mockRemotePreviewMgr = Mockito.mock(RemotePreviewManagerImpl.class);
		
		previewMgr = new PreviewManagerImpl(mockFileHandleDao, mockLocalPreviewMgr, mockRemotePreviewMgr);
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testCanHandleTypeInvalidContentType() {
		FileHandle expectedFileHandle = new S3FileHandle();
		expectedFileHandle.setId("id");
		expectedFileHandle.setContentType("contentType");
		expectedFileHandle.setFileName("name.ext");
		when(mockFileHandleDao.get("id")).thenReturn(expectedFileHandle);
		when(mockLocalPreviewMgr.canHandleType(anyString(), anyString())).thenReturn(false);
		when(mockRemotePreviewMgr.canHandleType(anyString(), anyString())).thenReturn(false);
		
		assertFalse(previewMgr.canHandleType("contentType", "ext"));
	}

	@Test
	public void testCanHandleTypeValidRemoteContentType() {
		FileHandle expectedFileHandle = new S3FileHandle();
		expectedFileHandle.setId("id");
		expectedFileHandle.setContentType("contentType");
		when(mockFileHandleDao.get("id")).thenReturn(expectedFileHandle);
		when(mockLocalPreviewMgr.canHandleType(anyString(), anyString())).thenReturn(false);
		when(mockRemotePreviewMgr.canHandleType(anyString(), anyString())).thenReturn(true);
		
		assertTrue(previewMgr.canHandleType("contentType", "ext"));
	}

	@Test
	public void testCanHandleTypeValidLocalContentType() {
		FileHandle expectedFileHandle = new S3FileHandle();
		expectedFileHandle.setId("id");
		expectedFileHandle.setContentType("contentType");
		when(mockFileHandleDao.get("id")).thenReturn(expectedFileHandle);
		when(mockLocalPreviewMgr.canHandleType(anyString(), anyString())).thenReturn(true);
		when(mockRemotePreviewMgr.canHandleType(anyString(), anyString())).thenReturn(true);
		
		assertTrue(previewMgr.canHandleType("contentType", "ext"));
	}
	
	@Test(expected=RuntimeException.class)
	public void testHandleInvalidContentType() throws Exception {
		S3FileHandle expectedFileHandle = new S3FileHandle();
		expectedFileHandle.setId("id");
		expectedFileHandle.setContentType("contentType");
		when(mockFileHandleDao.get("id")).thenReturn(expectedFileHandle);
		when(mockLocalPreviewMgr.canHandleType(anyString(), anyString())).thenReturn(false);
		when(mockRemotePreviewMgr.canHandleType(anyString(), anyString())).thenReturn(false);
		
		previewMgr.handle(expectedFileHandle);
		verify(mockLocalPreviewMgr, never()).handle(any(S3FileHandle.class));
		verify(mockRemotePreviewMgr, never()).handle(any(S3FileHandle.class));
	}

	@Test
	public void testHandleValidRemoteContentType() throws Exception {
		S3FileHandle expectedFileHandle = new S3FileHandle();
		expectedFileHandle.setId("id");
		expectedFileHandle.setContentType("contentType");
		when(mockFileHandleDao.get("id")).thenReturn(expectedFileHandle);
		when(mockLocalPreviewMgr.canHandleType(anyString(), anyString())).thenReturn(false);
		when(mockRemotePreviewMgr.canHandleType(anyString(), anyString())).thenReturn(true);
		
		previewMgr.handle(expectedFileHandle);
		verify(mockRemotePreviewMgr).handle(any(S3FileHandle.class));
		verify(mockLocalPreviewMgr, never()).handle(any(S3FileHandle.class));
	}

	@Test
	public void testHandleValidlocalContentType() throws Exception {
		S3FileHandle expectedFileHandle = new S3FileHandle();
		expectedFileHandle.setId("id");
		expectedFileHandle.setContentType("contentType");
		when(mockFileHandleDao.get("id")).thenReturn(expectedFileHandle);
		when(mockLocalPreviewMgr.canHandleType(anyString(), anyString())).thenReturn(true);
		when(mockRemotePreviewMgr.canHandleType(anyString(), anyString())).thenReturn(false);
		
		previewMgr.handle(expectedFileHandle);
		verify(mockLocalPreviewMgr).handle(any(S3FileHandle.class));
		verify(mockRemotePreviewMgr, never()).handle(any(S3FileHandle.class));
	}

}
