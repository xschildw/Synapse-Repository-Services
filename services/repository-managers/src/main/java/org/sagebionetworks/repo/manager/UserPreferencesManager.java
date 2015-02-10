package org.sagebionetworks.repo.manager;

import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserPreferences;
import org.sagebionetworks.repo.web.NotFoundException;

public interface UserPreferencesManager {
	
	/**
	 * Get own UserPreferences
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	UserPreferences getUserPreferences(UserInfo usrInfo) throws DatastoreException, NotFoundException;
	
	/**
	 * Create own UserPreferences
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	UserPreferences createUserPreferences(UserInfo usrInfo, UserPreferences prefs) throws DatastoreException, NotFoundException;
	
	/**
	 * Update own UserPreferences
	 * @throws NotFoundException 
	 * @throws ConflictingUpdateException 
	 * @throws InvalidModelException 
	 * @throws DatastoreException 
	 */
	UserPreferences updateUserPreferences(UserInfo usrInfo, UserPreferences prefs) throws DatastoreException, InvalidModelException, ConflictingUpdateException, NotFoundException;
	
	/**
	 * Delete own UserPreferences
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	void deleteUserPreferences(UserInfo usrInfo) throws DatastoreException, NotFoundException;

}
