package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.*;

import java.util.Date;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserPreferences;
import org.sagebionetworks.repo.model.UserPreferencesDAO;
import org.sagebionetworks.repo.model.message.Settings;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DBOUserPreferencesDAOImplTest {
	
	@Autowired 
	UserGroupDAO userGroupDAO;
	
	@Autowired
	UserPreferencesDAO userPreferencesDao;
	
	private UserGroup usr;

	@Before
	public void setUp() throws Exception {
		usr = new UserGroup();
		usr.setIsIndividual(true);
		usr.setCreationDate(new Date());
		usr.setId(userGroupDAO.create(usr).toString());
		
	}

	@After
	public void tearDown() throws Exception {
		if (usr != null) {
			// this will delete the user profile too
			userGroupDAO.delete(usr.getId());
		}
	}

	@Test
	public void testCRUD() throws DatastoreException, NotFoundException {
		UserPreferences userPrefs = new UserPreferences();
		userPrefs.setOwnerId(usr.getId());
		userPrefs.setEtag("etag");
		Settings notificationSettings = new Settings();
		notificationSettings.setMarkEmailedMessagesAsRead(true);
		notificationSettings.setSendEmailNotifications(true);
		userPrefs.setNotificationSettings(notificationSettings);
		
		String id = userPreferencesDao.create(userPrefs);
		assertNotNull(id);
		assertEquals(usr.getId(), id);
		
		UserPreferences up = userPreferencesDao.get(id);
		assertNotNull(up);
		assertEquals(userPrefs, up);
		
		UserPreferences up2 = userPreferencesDao.update(up);
		assertNotNull(up2);
		assertTrue("Etag should have been updated", ! up.getEtag().equals(up2.getEtag()));
		
		try {
			Settings s = new Settings();
			s.setMarkEmailedMessagesAsRead(false);
			s.setSendEmailNotifications(false);
			up.setNotificationSettings(s);
			userPreferencesDao.update(up);
			fail("Should have thrown a conflicting update exception");
		}
		catch(ConflictingUpdateException e) {
			// We expected this exception
		}

		// Delete it
		userPreferencesDao.delete(id);
		
	}

}
