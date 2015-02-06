package org.sagebionetworks.repo.model.dbo.persistence;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.sagebionetworks.repo.model.UserPreferences;
import org.sagebionetworks.repo.model.dbo.AutoTableMapping;
import org.sagebionetworks.repo.model.dbo.Field;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.Table;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.dao.UserPreferencesUtils;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.query.jdo.SqlConstants;

@Table(name = SqlConstants.TABLE_USER_PREFERENCES)
public class DBOUserPreferences implements MigratableDatabaseObject<DBOUserPreferences, DBOUserPreferences> {

	private static final TableMapping<DBOUserPreferences> tableMapping = AutoTableMapping
			.create(DBOUserPreferences.class);

	public static final String OWNER_ID_FIELD_NAME = "ownerId";

	@Override
	public TableMapping<DBOUserPreferences> getTableMapping() {
		return tableMapping;
	}

	@Field(name = SqlConstants.COL_USER_PREFERENCES_ID, primary = true, nullable = false, backupId=true)
	private Long ownerId;
	
	@Field(name = SqlConstants.COL_USER_PREFERENCES_ETAG, nullable = false, etag=true)
	private String etag;

	@Field(name = SqlConstants.COL_USER_PREFERENCES_PROPS_BLOB, serialized = "mediumblob", nullable = true)
	private byte[] propsBlob;

	public Long getOwnerId() {
		return ownerId;
	}

	public void setOwnerId(Long ownerId) {
		this.ownerId = ownerId;
	}

	public String getEtag() {
		return etag;
	}

	public void setEtag(String etag) {
		this.etag = etag;
	}

	public byte[] getPropsBlob() {
		return propsBlob;
	}

	public void setPropsBlob(byte[] propsBlob) {
		this.propsBlob = propsBlob;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((ownerId == null) ? 0 : ownerId.hashCode());
		result = prime * result + ((etag == null) ? 0 : etag.hashCode());
		result = prime * result + ((propsBlob == null) ? 0 : Arrays.hashCode(propsBlob));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DBOUserPreferences other = (DBOUserPreferences) obj;
		if (etag == null) {
			if (other.etag != null)
				return false;
		} else if (!etag.equals(other.etag))
			return false;
		if (ownerId == null) {
			if (other.ownerId != null)
				return false;
		} else if (!ownerId.equals(other.ownerId))
			return false;
		if (!Arrays.equals(propsBlob, other.propsBlob))
			return false;
		return true;
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.USER_PREFERENCES;
	}

	@Override
	public MigratableTableTranslation<DBOUserPreferences, DBOUserPreferences> getTranslator() {
		return new MigratableTableTranslation<DBOUserPreferences, DBOUserPreferences>(){

			@Override
			public DBOUserPreferences createDatabaseObjectFromBackup(
					DBOUserPreferences backup) {
//				UserPreferences up = UserPreferencesUtils.deserialize(backup.getPropsBlob());
//				try {
//					backup.setPropsBlob(JDOSecondaryPropertyUtils.compressObject(up));
//				} catch (IOException e) {
//					throw new RuntimeException(e);
//				}
				return backup;
			}

			@Override
			public DBOUserPreferences createBackupFromDatabaseObject(
					DBOUserPreferences dbo) {
				return dbo;
			}};
	}

	@Override
	public Class<? extends DBOUserPreferences> getBackupClass() {
		return DBOUserPreferences.class;
	}

	@Override
	public Class<? extends DBOUserPreferences> getDatabaseObjectClass() {
		return DBOUserPreferences.class;
	}

	@Override
	public List<MigratableDatabaseObject> getSecondaryTypes() {
		return null;
	}

}
