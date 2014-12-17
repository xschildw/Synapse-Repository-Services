package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.Date;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.ProjectSettingsDAO;
import org.sagebionetworks.repo.model.project.ProjectSetting;
import org.sagebionetworks.repo.model.project.UploadDestinationListSetting;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DBOProjectSettingsDAOImplTest {

	@Autowired
	NodeDAO nodeDao;

	@Autowired
	ProjectSettingsDAO projectSettingsDao;

	private String projectId;

	@Before
	public void setup() throws Exception {
		Long userId = BOOTSTRAP_PRINCIPAL.TEST_ADMIN_USER.getPrincipalId();

		Node project = new Node();
		project.setName("project");
		project.setNodeType(EntityType.getNodeTypeForClass(Project.class).name());
		project.setCreatedByPrincipalId(userId);
		project.setCreatedOn(new Date());
		project.setModifiedByPrincipalId(userId);
		project.setModifiedOn(new Date());
		projectId = nodeDao.createNew(project);
	}

	@After
	public void teardown() throws Exception {
		if (projectId != null) {
			nodeDao.delete(projectId);
		}
	}

	@Test
	public void testCRUD() throws Exception {
		// Create a new type
		UploadDestinationListSetting setting = new UploadDestinationListSetting();
		setting.setEtag("etag");
		setting.setId(null);
		setting.setProjectId(projectId);
		setting.setSettingsType("upload");

		// there should not be a settings to begin with
		assertNull(projectSettingsDao.get(projectId, "upload"));
		assertEquals(0, projectSettingsDao.getAllForProject(projectId).size());

		// Create it
		String id = projectSettingsDao.create(setting);
		setting.setId(id);
		assertNotNull(id);

		// Fetch it
		ProjectSetting clone = projectSettingsDao.get(projectId, "upload");
		assertNotNull(clone);
		assertEquals(setting, clone);

		// Fetch it by id
		ProjectSetting clone2 = projectSettingsDao.get(id);
		assertEquals(setting, clone2);

		// Fetch all by project
		List<ProjectSetting> all = projectSettingsDao.getAllForProject(projectId);
		assertEquals(1, all.size());
		assertEquals(setting, all.get(0));

		// Update it
		ProjectSetting updatedClone = projectSettingsDao.update(clone);
		assertNotSame("etags should be different after an update", clone.getEtag(), updatedClone.getEtag());

		try {
			projectSettingsDao.update(clone);
			fail("conflicting update exception not thrown");
		} catch (ConflictingUpdateException e) {
			// We expected this exception
		}

		// Delete it
		projectSettingsDao.delete(id);

		assertNull(projectSettingsDao.get(projectId, "upload"));
		assertEquals(0, projectSettingsDao.getAllForProject(projectId).size());
	}

	@Test
	public void testCascadeDelete() throws Exception {
		UploadDestinationListSetting setting = new UploadDestinationListSetting();
		setting.setEtag("etag");
		setting.setId(null);
		setting.setProjectId(projectId);
		setting.setSettingsType("upload");
		projectSettingsDao.create(setting);
		assertEquals(1, projectSettingsDao.getAllForProject(projectId).size());

		nodeDao.delete(projectId);
		assertEquals(0, projectSettingsDao.getAllForProject(projectId).size());
		projectId = null;
	}

	@Test(expected = InvalidModelException.class)
	public void testProjectIdMustBeSet() {
		UploadDestinationListSetting setting = new UploadDestinationListSetting();
		setting.setProjectId(null);
		setting.setSettingsType("upload");
		projectSettingsDao.create(setting);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testProjectIdMustBeValid() {
		UploadDestinationListSetting setting = new UploadDestinationListSetting();
		setting.setProjectId("123");
		setting.setSettingsType("upload");
		projectSettingsDao.create(setting);
	}

	@Test(expected = InvalidModelException.class)
	public void testTypeMustBeSet() {
		UploadDestinationListSetting setting = new UploadDestinationListSetting();
		setting.setProjectId(projectId);
		setting.setSettingsType(null);
		projectSettingsDao.create(setting);
	}
}
