package org.sagebionetworks.repo.web.service;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.repo.model.UserPreferences;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.http.HttpHeaders;

public interface UserPreferencesService {
	
	/**
	 * Get user preferences of specified user
	 * @param userId
	 * @return
	 * @throws NotFoundException 
	 */
	public UserPreferences getUserPreferences(Long userId) throws NotFoundException;
	
	/**
	 * Create user preferences for specified user
	 * @param userId
	 * @param prefs
	 * @return
	 * @throws NotFoundException 
	 * @throws IOException 
	 */
	UserPreferences createUserPreferences(Long userId, UserPreferences userPreferences)
			throws NotFoundException, IOException;

	
	/**
	 * Update user preferences for specified user
	 * @param userId
	 * @param prefs
	 * @return
	 * @throws NotFoundException 
	 * @throws IOException 
	 */
	UserPreferences updateUserPreferences(Long userId, UserPreferences userPreferences)
			throws NotFoundException, IOException;
	
	/**
	 * Delete user preferences for specified user
	 * @param userId
	 * @throws NotFoundException 
	 */
	public void deleteUserPreferences(Long userId) throws NotFoundException;


}
