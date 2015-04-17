package org.sagebionetworks.repo.manager;

import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserPreferences;
import org.sagebionetworks.repo.model.UserPreferencesDAO;
import org.sagebionetworks.repo.model.message.Settings;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

public class UserPreferencesManagerImpl implements UserPreferencesManager {
	
	@Autowired
	UserPreferencesDAO userPreferencesDAO;
	
	@Autowired
	private AuthorizationManager authorizationManager;
	
	public UserPreferencesManagerImpl() {		
	}
	
	// Constructor for unit tests
	public UserPreferencesManagerImpl(UserPreferencesDAO upDAO, AuthorizationManager authMgr) {
		this.userPreferencesDAO = upDAO;
		this.authorizationManager = authMgr;
	}

	@Override
	public UserPreferences getUserPreferences(UserInfo usrInfo) throws DatastoreException {
		if (usrInfo == null) { 
			throw new IllegalArgumentException("usrInfo cannot be null.");
		}
		// If user does not have preferences record yet, return default
		UserPreferences p = null;
		try {
			p = userPreferencesDAO.get(usrInfo.getId().toString());
		} catch (NotFoundException e) {
			UserPreferences prefs = new UserPreferences();
			prefs.setOwnerId(usrInfo.getId().toString());
			Settings s = new Settings();
			s.setMarkEmailedMessagesAsRead(false);
			s.setSendEmailNotifications(false);
			prefs.setNotificationSettings(s);
			prefs.setUri(null);
			prefs.setEtag(null);
			p = prefs;
		}
		return p;
	}

	@WriteTransaction
	@Override
	public UserPreferences createUserPreferences(UserInfo usrInfo,
			UserPreferences prefs) throws DatastoreException, NotFoundException {
		if (usrInfo == null) { 
			throw new IllegalArgumentException("usrInfo cannot be null.");
		}
		String id = userPreferencesDAO.create(prefs);
		// TODO: Would it be better for the DAO to return the created object instead of its ID?
		UserPreferences cPrefs = userPreferencesDAO.get(id);
		return cPrefs;
	}

	@WriteTransaction
	@Override
	public UserPreferences updateUserPreferences(UserInfo usrInfo,
			UserPreferences prefs) throws DatastoreException, InvalidModelException, ConflictingUpdateException, NotFoundException {
		if (usrInfo == null) { 
			throw new IllegalArgumentException("usrInfo cannot be null.");
		}
		UserPreferences uPrefs = userPreferencesDAO.update(prefs);
		return uPrefs;
	}

	@Override
	public void deleteUserPreferences(UserInfo usrInfo) throws DatastoreException, NotFoundException {
		if (usrInfo == null) { 
			throw new IllegalArgumentException("usrInfo cannot be null.");
		}
		userPreferencesDAO.delete(usrInfo.getId().toString());
	}

}
