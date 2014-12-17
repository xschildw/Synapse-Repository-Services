package org.sagebionetworks.file.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.URL;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.file.ExternalFileHandle;
import org.sagebionetworks.repo.model.file.PreviewFileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.controller.AbstractAutowiredControllerTestBase;
import org.springframework.beans.factory.annotation.Autowired;

public class UploadControllerTest extends AbstractAutowiredControllerTestBase {
	
	@Autowired
	private FileHandleDao fileMetadataDao;
	
	private Long adminUserId;
	private String adminUserIdString;

	private S3FileHandle handleOne;
	private PreviewFileHandle handleTwo;
	private List<String> toDelete;

	@Before
	public void before() throws Exception {
		toDelete = new LinkedList<String>();
		
		// get user IDs
		adminUserId = BOOTSTRAP_PRINCIPAL.TEST_ADMIN_USER.getPrincipalId();
		adminUserIdString = adminUserId.toString();
		
		// Create a file handle
		handleOne = new S3FileHandle();
		handleOne.setCreatedBy(adminUserIdString);
		handleOne.setCreatedOn(new Date());
		handleOne.setBucketName("bucket");
		handleOne.setKey("mainFileKey");
		handleOne.setEtag("etag");
		handleOne.setFileName("foo.bar");
		handleOne = fileMetadataDao.createFile(handleOne);
		toDelete.add(handleOne.getId());
		// Create a preview
		handleTwo = new PreviewFileHandle();
		handleTwo.setCreatedBy(adminUserIdString);
		handleTwo.setCreatedOn(new Date());
		handleTwo.setBucketName("bucket");
		handleTwo.setKey("previewFileKey");
		handleTwo.setEtag("etag");
		handleTwo.setFileName("bar.txt");
		handleTwo = fileMetadataDao.createFile(handleTwo);
		// Set two as the preview of one
		fileMetadataDao.setPreviewId(handleOne.getId(), handleTwo.getId());
		toDelete.add(handleTwo.getId());
	}

	@After
	public void after() throws Exception {
		for (String id : toDelete) {
			fileMetadataDao.delete(id);
		}
	}

	@Test
	public void testGetFileHandle() throws Exception {
		S3FileHandle handle = servletTestHelper.getFileHandle(adminUserId, handleOne.getId());
		assertEquals(handleOne.getId(), handle.getId());
		// Should also be able to get the URL
		URL url = servletTestHelper.getFileHandleUrl(adminUserId, handleOne.getId(), false);
		assertNotNull(url);
		String one = url.toString();
		assertTrue(one.contains(handle.getKey()));
		// With redirect this time
		url = servletTestHelper.getFileHandleUrl(adminUserId, handleOne.getId(), true);
		assertNotNull(url);
		String two = url.toString();
		assertTrue(two.contains(handle.getKey()));
	}
	
	@Test
	public void testClearPreview() throws Exception {
		servletTestHelper.deleteFilePreview(adminUserId, handleOne.getId());
	}

	@Test
	public void testExternalFileHandle() throws Exception {
		ExternalFileHandle efh = new ExternalFileHandle();
		efh.setExternalURL("http://www.google.com");
		ExternalFileHandle returned = servletTestHelper.createExternalFileHandle(adminUserId, efh);
		assertNotNull(returned);
		toDelete.add(returned.getId());
		assertEquals(efh.getExternalURL(), returned.getExternalURL());
	}
	
	@Test
	public void testPLFM_1944() throws Exception{
		try {
			servletTestHelper.getFileHandle(adminUserId, handleOne.getId());
		} catch (NotFoundException e) { }
	}
}
