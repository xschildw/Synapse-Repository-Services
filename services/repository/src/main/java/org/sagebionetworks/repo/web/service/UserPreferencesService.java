package org.sagebionetworks.repo.web.service;

import org.sagebionetworks.repo.model.UserPreferences;
import org.sagebionetworks.repo.web.NotFoundException;

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
	 */
	public UserPreferences createUserPreferences(Long userId, UserPreferences prefs) throws NotFoundException;
	
	/**
	 * Update user preferences for specified user
	 * @param userId
	 * @param prefs
	 * @return
	 * @throws NotFoundException 
	 */
	public UserPreferences updateUserPreferences(Long userId, UserPreferences prefs) throws NotFoundException;
	
	/**
	 * Delete user preferences for specified user
	 * @param userId
	 * @throws NotFoundException 
	 */
	public void deleteUserPreferences(Long userId) throws NotFoundException;

}
