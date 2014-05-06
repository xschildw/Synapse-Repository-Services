package org.sagebionetworks.repo.model;

import java.util.List;

import org.sagebionetworks.repo.web.NotFoundException;

public interface UserPreferencesDAO {

	/**
	 * @param dto
	 *            object to be created
	 * @return the id of the newly created object
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 */
	public String create(UserPreferences dto) throws DatastoreException,
			InvalidModelException;

	/**
	 * Retrieves a subset of preferences for a given user
	 * 
	 * @param id
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public UserPreferences get(String userId, List<String> preferenceNames) throws DatastoreException, NotFoundException;
	
	/**
	 * 
	 * Get all the UserPreferences for given user
	 * 
	 * @param startIncl
	 * @param endExcl
	 * @param sort
	 * @param ascending
	 * @param schema
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public UserPreferences get(String userId) throws DatastoreException, NotFoundException;

	/**
	 * Get the total count of UserProfiles in the system
	 * 
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public long getCount() throws DatastoreException, NotFoundException;

	/**
	 * Updates the 'shallow' properties of an object.
	 *
	 * @param dto
	 *            non-null id is required
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 * @throws NotFoundException
	 */
	public UserPreferences update(UserPreferences dto) throws DatastoreException, InvalidModelException,
			NotFoundException, ConflictingUpdateException;

	/**
	 * Delete the given preferences for the given user
	 * 
	 * @param id
	 *            the id of the object to be deleted
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public void delete(String userId, List<String> preferenceNames) throws DatastoreException, NotFoundException;
	
	/**
	 * Ensure the bootstrap user's profiles exist
	 */
	public void bootstrapUserPreferences();
}
