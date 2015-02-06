package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.*;

import org.junit.Test;

import org.sagebionetworks.repo.model.UserPreferences;
import org.sagebionetworks.repo.model.dbo.persistence.DBOUserPreferences;
import org.sagebionetworks.repo.model.message.Settings;

public class UserPreferencesUtilsTest {

	@Test
	public void testRoundTrip() {
		UserPreferences dto = new UserPreferences();
		dto.setOwnerId("1000");
		dto.setEtag("eTag");
		Settings notificationSettings = new Settings();
		notificationSettings.setMarkEmailedMessagesAsRead(true);
		notificationSettings.setSendEmailNotifications(false);
		dto.setNotificationSettings(notificationSettings);
		
		DBOUserPreferences dbo = new DBOUserPreferences();
		UserPreferencesUtils.copyDtoToDbo(dto, dbo);
		UserPreferences dto2 = UserPreferencesUtils.convertDboToDto(dbo);
		assertEquals(dto, dto2);
	}

}
