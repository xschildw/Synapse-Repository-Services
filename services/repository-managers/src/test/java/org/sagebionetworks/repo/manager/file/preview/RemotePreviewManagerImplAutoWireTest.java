package org.sagebionetworks.repo.manager.file.preview;

import static org.junit.Assert.*;

import java.util.LinkedList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.file.S3FileHandleInterface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.amazonaws.services.s3.AmazonS3Client;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class RemotePreviewManagerImplAutoWireTest {
	
	@Autowired
	private FileHandleManager fileUploadManager;

	@Autowired
	private RemotePreviewManager remotePreviewManager;

	@Autowired
	public UserManager userManager;

	@Autowired
	private AmazonS3Client s3Client;

	@Autowired
	private FileHandleDao fileMetadataDao;

	// Only used to satisfy FKs
	private UserInfo adminUserInfo;
	private List<S3FileHandleInterface> toDelete = new LinkedList<S3FileHandleInterface>();

	private static String LITTLE_IMAGE_NAME = "LittleImage.png";
	private static final String CSV_TEXT_FILE = "images/test.csv";

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testCanHandle() {
		assertFalse(remotePreviewManager.canHandleType("image/gif", ".gif"));
		assertFalse(remotePreviewManager.canHandleType("application/pdf", ".pdf"));
	}

}
