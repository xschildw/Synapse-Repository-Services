package org.sagebionetworks.repo.web.service;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.UserPreferencesManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserPreferences;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.controller.ObjectTypeSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;

public class UserPreferencesServiceImpl implements UserPreferencesService {

	private final Logger logger = LogManager.getLogger(UserPreferencesServiceImpl.class);
	
	@Autowired
	private UserManager userManager;
	
	@Autowired
	private UserPreferencesManager userPreferencesManager;

	@Autowired
	private ObjectTypeSerializer objectTypeSerializer;
	
	@Override
	public UserPreferences getUserPreferences(Long userId) throws NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return userPreferencesManager.getUserPreferences(userInfo);
	}

	@Override
	public UserPreferences createUserPreferences(Long userId,
			HttpHeaders header, HttpServletRequest request) throws NotFoundException, IOException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		UserPreferences prefs = (UserPreferences) objectTypeSerializer.deserialize(request.getInputStream(), header, UserPreferences.class, header.getContentType());
		return userPreferencesManager.createUserPreferences(userInfo, prefs);
	}

	@Override
	public UserPreferences updateUserPreferences(Long userId,
			HttpHeaders header, HttpServletRequest request) throws NotFoundException, IOException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		UserPreferences prefs = (UserPreferences) objectTypeSerializer.deserialize(request.getInputStream(), header, UserPreferences.class, header.getContentType());
		return userPreferencesManager.updateUserPreferences(userInfo, prefs);
	}

	@Override
	public void deleteUserPreferences(Long userId) throws NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		userPreferencesManager.deleteUserPreferences(userInfo);		
	}

}
