package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.*;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertFalse;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.mockito.Mockito;
import static org.mockito.Mockito.when;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.UserPreferences;
import org.sagebionetworks.repo.model.dbo.principal.PrincipalPrefixDAO;
import org.sagebionetworks.repo.model.message.Settings;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.service.EntityService;
import org.sagebionetworks.repo.web.service.UserPreferencesService;
import org.springframework.beans.factory.annotation.Autowired;

public class UserPreferencesControllerAutowiredTest extends AbstractAutowiredControllerTestBase {
	
	@Autowired
	private UserPreferencesService userPreferencesService;
	
	@Autowired
	private PrincipalPrefixDAO principalPrefixDao;

	@Autowired 
	private EntityService entityService;
	
	private Long adminUserId;
	private List<String> entityIdsToDelete;

	HttpServletRequest mockRequest;

	@Before
	public void setUp() throws Exception {
		adminUserId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
		
		assertNotNull(userPreferencesService);

		mockRequest = Mockito.mock(HttpServletRequest.class);
		when(mockRequest.getServletPath()).thenReturn("/repo/v1");

	}

	@After
	public void tearDown() throws Exception {
		if (entityService != null && entityIdsToDelete != null) {
			for (String idToDelete : entityIdsToDelete) {
				try {
					entityService.deleteEntity(adminUserId, idToDelete);
				} catch (NotFoundException e) {
					// nothing to do here
				} catch (DatastoreException e) {
					// nothing to do here.
				}
			}
		}
		// Delete user preference record
		userPreferencesService.deleteUserPreferences(adminUserId);
	}
	
	@Test
	public void testUserPreferencesCRUD() throws Exception {
		// Get default record
		UserPreferences expectedUserPrefs = servletTestHelper.getUserPreferences(dispatchServlet, adminUserId);
		assertNotNull(expectedUserPrefs);
		Settings s = expectedUserPrefs.getNotificationSettings();
		assertNotNull(s);
		assertFalse(s.getMarkEmailedMessagesAsRead());
		assertFalse(s.getSendEmailNotifications());
		assertNotNull(expectedUserPrefs.getOwnerId());
		Long l = Long.parseLong(expectedUserPrefs.getOwnerId());
		assertEquals(adminUserId, l);
		assertNull(expectedUserPrefs.getEtag());
		// Create Record
		s.setMarkEmailedMessagesAsRead(true);
		UserPreferences createdUserPrefs = servletTestHelper.createUserPreferences(dispatchServlet, expectedUserPrefs, adminUserId);
		assertNotNull(createdUserPrefs);
		Settings s2 = createdUserPrefs.getNotificationSettings();
		assertNotNull(s2);
		assertTrue(s2.getMarkEmailedMessagesAsRead());
		assertFalse(s2.getSendEmailNotifications());
		assertNotNull(createdUserPrefs.getOwnerId());
		Long l2 = Long.parseLong(createdUserPrefs.getOwnerId());
		assertEquals(adminUserId, l2);
		assertNotNull(createdUserPrefs.getEtag());
		// Read it
		UserPreferences readUserPrefs = servletTestHelper.getUserPreferences(dispatchServlet, adminUserId);
		assertNotNull(expectedUserPrefs);
		Settings s3 = expectedUserPrefs.getNotificationSettings();
		assertNotNull(s3);
		assertEquals(s2.getMarkEmailedMessagesAsRead(), s3.getMarkEmailedMessagesAsRead());
		assertEquals(s2.getSendEmailNotifications(), s3.getSendEmailNotifications());
		assertEquals(createdUserPrefs.getOwnerId(), readUserPrefs.getOwnerId());
		assertEquals(createdUserPrefs.getEtag(), readUserPrefs.getEtag());
		// Update it
		readUserPrefs.getNotificationSettings().setSendEmailNotifications(true);
		UserPreferences updatedUserPrefs = servletTestHelper.updateUserPreferences(dispatchServlet, readUserPrefs, adminUserId);
		assertNotNull(updatedUserPrefs);
		Settings s4 = updatedUserPrefs.getNotificationSettings();
		assertNotNull(s4);
		assertTrue(s4.getMarkEmailedMessagesAsRead());
		assertTrue(s4.getSendEmailNotifications());
		assertEquals(readUserPrefs.getOwnerId(), updatedUserPrefs.getOwnerId());
		assertFalse(readUserPrefs.getEtag().equals(updatedUserPrefs.getEtag()));
		
	}

}
