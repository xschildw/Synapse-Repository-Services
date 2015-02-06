package org.sagebionetworks.repo.model.dbo.dao;

import java.util.UUID;

import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UserPreferences;
import org.sagebionetworks.repo.model.UserPreferencesDAO;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.SinglePrimaryKeySqlParameterSource;
import org.sagebionetworks.repo.model.dbo.persistence.DBOUserPreferences;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.TransactionalMessenger;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public class DBOUserPreferencesDAOImpl implements UserPreferencesDAO {

	@Autowired
	private DBOBasicDao basicDao;
	
	@Autowired
	private TransactionalMessenger transactionalMessenger;
	

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public String create(UserPreferences dto) throws DatastoreException {
		DBOUserPreferences dbo = new DBOUserPreferences();
		UserPreferencesUtils.copyDtoToDbo(dto, dbo);
		if (dbo.getEtag() == null) {
			dbo.setEtag(UUID.randomUUID().toString());
		}
		dbo = basicDao.createNew(dbo);
		return dbo.getOwnerId().toString();
	}

	@Override
	public UserPreferences get(String id) throws DatastoreException, NotFoundException {
		DBOUserPreferences dbo = basicDao.getObjectByPrimaryKey(DBOUserPreferences.class,
				new SinglePrimaryKeySqlParameterSource(id));
		UserPreferences dto = UserPreferencesUtils.convertDboToDto(dbo);
		return dto;
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public UserPreferences update(UserPreferences dto)
			throws DatastoreException, InvalidModelException,
			NotFoundException, ConflictingUpdateException {
		DBOUserPreferences dbo = basicDao.getObjectByPrimaryKey(DBOUserPreferences.class,
				new SinglePrimaryKeySqlParameterSource(dto.getOwnerId()));
		if (!dbo.getEtag().equals(dto.getEtag())) {
			throw new ConflictingUpdateException(
					"UserPpreferences was updated since you last fetched it, retrieve it again and reapply the update.");
		}
		UserPreferencesUtils.copyDtoToDbo(dto, dbo);
		dbo.setEtag(UUID.randomUUID().toString());
		transactionalMessenger.sendMessageAfterCommit("" + dbo.getOwnerId(), ObjectType.PRINCIPAL, dbo.getEtag(), ChangeType.UPDATE);
		boolean success = basicDao.update(dbo);
		if (!success) {
			throw new DatastoreException("Unsuccessful updating user preferences in database.");
		}
		UserPreferences resultantDto = UserPreferencesUtils.convertDboToDto(dbo);

		return resultantDto;
		
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void delete(String id) throws DatastoreException, NotFoundException {
		basicDao.deleteObjectByPrimaryKey(DBOUserPreferences.class,
				new SinglePrimaryKeySqlParameterSource(id));
		
	}

}
