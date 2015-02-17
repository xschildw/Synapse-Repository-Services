package org.sagebionetworks.repo.web.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.UserPreferencesManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserPreferences;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

public class UserPreferencesServiceImpl implements UserPreferencesService {

	private final Logger logger = LogManager.getLogger(UserPreferencesServiceImpl.class);
	
	@Autowired
	private UserManager userManager;
	
	@Autowired
	private UserPreferencesManager userPreferencesManager;
	
	@Override
	public UserPreferences getUserPreferences(Long userId) throws NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return userPreferencesManager.getUserPreferences(userInfo);
	}

	@Override
	public UserPreferences createUserPreferences(Long userId,
			UserPreferences prefs) throws NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return userPreferencesManager.createUserPreferences(userInfo, prefs);
	}

	@Override
	public UserPreferences updateUserPreferences(Long userId,
			UserPreferences prefs) throws NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return userPreferencesManager.updateUserPreferences(userInfo, prefs);
	}

	@Override
	public void deleteUserPreferences(Long userId) throws NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		userPreferencesManager.deleteUserPreferences(userInfo);		
	}

}
