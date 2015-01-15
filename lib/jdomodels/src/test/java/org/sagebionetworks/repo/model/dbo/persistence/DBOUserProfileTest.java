package org.sagebionetworks.repo.model.dbo.persistence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.dao.UserProfileUtils;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;
import org.sagebionetworks.repo.model.message.Settings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DBOUserProfileTest {
	
	@Autowired
	DBOBasicDao dboBasicDao;
	
	@Autowired 
	UserGroupDAO userGroupDAO;
		
	private UserGroup individualGroup = null;
	
	
	@Before
	public void setUp() throws Exception {
		individualGroup = new UserGroup();
		individualGroup.setIsIndividual(true);
		individualGroup.setCreationDate(new Date());
		individualGroup.setId(userGroupDAO.create(individualGroup).toString());
		deleteUserProfile();
	}
	
	private void deleteUserProfile() throws DatastoreException {
		if(dboBasicDao != null && individualGroup!=null){
			MapSqlParameterSource params = new MapSqlParameterSource();
			params.addValue("ownerId", individualGroup.getId());
			dboBasicDao.deleteObjectByPrimaryKey(DBOUserProfile.class, params);
		}		
	}
		
	
	@After
	public void tearDown() throws Exception{
		if (individualGroup != null) {
			// this will delete the user profile too
			userGroupDAO.delete(individualGroup.getId());
		}
	}
	
	@Test
	public void testCRUD() throws Exception{
		// Create a new type
		DBOUserProfile userProfile = new DBOUserProfile();
		userProfile.setOwnerId(Long.parseLong(individualGroup.getId()));
		userProfile.seteTag("10");
		userProfile.setProperties("My dog has fleas.".getBytes());
		
		// Create it
		DBOUserProfile clone = dboBasicDao.createNew(userProfile);
		assertNotNull(clone);
		assertEquals(userProfile, clone);
		// Fetch it
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("ownerId", individualGroup.getId());
		clone = dboBasicDao.getObjectByPrimaryKey(DBOUserProfile.class, params);
		assertNotNull(clone);
		assertEquals(userProfile.getOwnerId(), clone.getOwnerId());
		
		// Update it
		String newContent = "Your dog has fleas!";
		clone.setProperties(newContent.getBytes());
		dboBasicDao.update(clone);
		clone = dboBasicDao.getObjectByPrimaryKey(DBOUserProfile.class, params);
		assertNotNull(clone);
		assertEquals(newContent, new String(clone.getProperties()));
		
		// Delete it
		boolean result = dboBasicDao.deleteObjectByPrimaryKey(DBOUserProfile.class,  params);
		assertTrue("Failed to delete the type created", result);
		
	}
	
	@Test
	public void testMigrationTranslator() throws Exception {
		// Create user profile
		UserProfile userProfile = new UserProfile();
		userProfile.setDisplayName("userDisplayName");
		userProfile.setEmail("emailShouldBeDeleted");
		userProfile.setUserName("userNameShouldBeDeleted");
		List<String> emails = new ArrayList<String>();
		emails.add("email1ShouldBeDeleted");
		emails.add("email2ShouldBeDeleted");
		userProfile.setEmails(emails);
		List<String> openIds = new ArrayList<String>();
		openIds.add("openId1ShouldBeDeleted");
		userProfile.setOpenIds(openIds);
		Settings notificationSettings = new Settings();
		notificationSettings.setSendEmailNotifications(false);
		userProfile.setNotificationSettings(notificationSettings);
		// Backup
		byte[] userProfileSerialized = JDOSecondaryPropertyUtils.compressObject(userProfile);
		DBOUserProfile backup = new DBOUserProfile();
		backup.setOwnerId(Long.parseLong(individualGroup.getId()));
		backup.seteTag("xxx");
		backup.setProperties(userProfileSerialized);
		MigratableTableTranslation<DBOUserProfile, DBOUserProfile> translator = backup.getTranslator();
		// Restore
		DBOUserProfile dbo = translator.createDatabaseObjectFromBackup(backup);
		UserProfile restoredUserProfile = UserProfileUtils.deserialize(dbo.getProperties());
		assertNull(restoredUserProfile.getEmail());
		assertNull(restoredUserProfile.getEmails());
		assertNull(restoredUserProfile.getOpenIds());
		assertNull(restoredUserProfile.getUserName());
		assertEquals(userProfile.getDisplayName(), restoredUserProfile.getDisplayName());
		assertNotNull(restoredUserProfile.getPreferences());
		assertNotNull(restoredUserProfile.getPreferences().getNotificationSettings());
		assertEquals(userProfile.getNotificationSettings().getSendEmailNotifications(), restoredUserProfile.getPreferences().getNotificationSettings().getSendEmailNotifications());
	}

}
