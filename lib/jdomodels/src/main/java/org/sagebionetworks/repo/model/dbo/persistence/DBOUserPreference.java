package org.sagebionetworks.repo.model.dbo.persistence;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_USERPREF_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_USERPREF_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_USERPREF_OWNER_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_USERPREF_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_USERPREF_VALUE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_USERPREF;

import java.util.List;

import org.sagebionetworks.repo.model.dbo.AutoTableMapping;
import org.sagebionetworks.repo.model.dbo.Field;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.Table;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;

@Table(name = TABLE_USERPREF)
public class DBOUserPreference implements MigratableDatabaseObject<DBOUserPreference, DBOUserPreference> {
	// TODO: Add id for migration?
	@Field(name=COL_USERPREF_OWNER_ID, backupId=false, primary=true, nullable=false)
	// TODO: Add foreign key
	private Long ownerId;
	@Field(name=COL_USERPREF_ETAG, backupId=false, primary=false, nullable=false)
	private String eTag;
	@Field(name=COL_USERPREF_NAME, backupId=false, primary=true, nullable=false)
	private String preferenceName;
	@Field(name=COL_USERPREF_TYPE, backupId=false, primary=false, nullable=false)
	private String preferenceType;
	@Field(name=COL_USERPREF_VALUE, backupId=false, primary=false, nullable=false)
	private String preferenceValue;
	
	private static final TableMapping<DBOUserPreference> TABLE_MAPPING = AutoTableMapping.create(DBOUserPreference.class);

	@Override
	public TableMapping<DBOUserPreference> getTableMapping() {
		return TABLE_MAPPING;
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.USER_PREFERENCE;
	}

	@Override
	public MigratableTableTranslation<DBOUserPreference, DBOUserPreference> getTranslator() {
		// We do not currently have a backup for this object.
		return new MigratableTableTranslation<DBOUserPreference, DBOUserPreference>() {

			@Override
			public DBOUserPreference createDatabaseObjectFromBackup(DBOUserPreference backup) {
				return backup;
			}

			@Override
			public DBOUserPreference createBackupFromDatabaseObject(DBOUserPreference dbo) {
				return dbo;
			}
		};

	}

	@Override
	public Class<? extends DBOUserPreference> getBackupClass() {
		return DBOUserPreference.class;
	}

	@Override
	public Class<? extends DBOUserPreference> getDatabaseObjectClass() {
		return DBOUserPreference.class;
	}

	@Override
	public List<MigratableDatabaseObject> getSecondaryTypes() {
		// TODO Auto-generated method stub
		return null;
	}

	public long getOwnerId() {
		return this.ownerId;
	}
	
	public void setOwnerId(long id) {
		this.ownerId = id;
	}
	
	public String geteTag() {
		return this.eTag;
	}
	
	public void seteTag(String e) {
		this.eTag = e;
	}

	public String getPreferenceName() {
		return this.preferenceName;
	}
	
	public void setPreferenceName(String name) {
		this.preferenceName = name;
	}
	
	public String getPreferenceType() {
		return this.preferenceType;
	}
	
	public void setPreferenceType(String type) {
		this.preferenceType = type;
	}
	
	public String getPreferenceValue() {
		return this.preferenceValue;
	}
	
	public void setPreferenceValue(String value) {
		this.preferenceValue = value;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((eTag == null) ? 0 : eTag.hashCode());
		result = prime * result
				+ ((ownerId == null) ? 0 : ownerId.hashCode());
		result = prime * result + ((preferenceName == null) ? 0 : preferenceName.hashCode());
		result = prime * result + ((preferenceType == null) ? 0 : preferenceType.hashCode());
		result = prime * result + ((preferenceValue == null) ? 0 : preferenceValue.hashCode());
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
		DBOUserPreference other = (DBOUserPreference) obj;
		if (eTag == null) {
			if (other.eTag != null)
				return false;
		} else if (!eTag.equals(other.eTag))
			return false;
		if (ownerId == null) {
			if (other.ownerId != null)
				return false;
		} else if (!ownerId.equals(other.ownerId))
			return false;
		if (preferenceName == null) {
			if (other.preferenceName != null)
				return false;
		} else if (!preferenceName.equals(other.preferenceName))
			return false;
		if (preferenceType == null) {
			if (other.preferenceType != null)
				return false;
		} else if (!preferenceType.equals(other.preferenceType))
			return false;
		if (preferenceValue == null) {
			if (other.preferenceValue != null)
				return false;
		} else if (!preferenceValue.equals(other.preferenceValue))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "DBOUserPreference [ownerId=" + ownerId + ", etag=" + eTag
				+ ", preferenceName=" + preferenceName
				+ ", preferenceType=" + preferenceType
				+ ", preferencevalue=" + preferenceValue
				+ "]";
	}

}
