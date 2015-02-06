package org.sagebionetworks.repo.model.dbo.dao;

import java.io.IOException;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UserPreferences;
import org.sagebionetworks.repo.model.dbo.persistence.DBOUserPreferences;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;

public class UserPreferencesUtils {
	
	public static void copyDtoToDbo(UserPreferences dto, DBOUserPreferences dbo) throws DatastoreException{
		if (dto.getOwnerId()==null) {
			dbo.setOwnerId(null);
		} else {
			dbo.setOwnerId(Long.parseLong(dto.getOwnerId()));
		}
		dbo.setEtag(dto.getEtag());
		try {
			dbo.setPropsBlob(JDOSecondaryPropertyUtils.compressObject(dto));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static UserPreferences deserialize(byte[] b) {
		Object decompressed = null;
		try {
			decompressed = JDOSecondaryPropertyUtils.decompressedObject(b);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
		UserPreferences dto = null;
		if (decompressed instanceof UserPreferences) {
			dto = (UserPreferences) decompressed;
		} else {
			throw new RuntimeException("Unsupported object type " + decompressed.getClass());
		}
		return dto;
	}
	
	public static UserPreferences convertDboToDto(DBOUserPreferences dbo) throws DatastoreException {
		UserPreferences dto = deserialize(dbo.getPropsBlob());
		if (dbo.getOwnerId()==null) {
			dto.setOwnerId(null);
		} else {
			dto.setOwnerId(dbo.getOwnerId().toString());
		}
		dto.setEtag(dbo.getEtag());
		return dto;
	}
	
	
}
