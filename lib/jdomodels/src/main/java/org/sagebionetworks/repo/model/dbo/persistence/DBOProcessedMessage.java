package org.sagebionetworks.repo.model.dbo.persistence;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.*;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import org.sagebionetworks.repo.model.dbo.DatabaseObject;
import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.TableMapping;

public class DBOProcessedMessage implements DatabaseObject<DBOProcessedMessage> {

	private static FieldColumn[] FIELDS = new FieldColumn[]{
		new FieldColumn("changeNumber", COL_PROCESSED_MESSAGES_CHANGE_NUM, true),
		new FieldColumn("timeStamp", COL_PROCESSED_MESSAGES_TIME_STAMP),
		new FieldColumn("processedBy", COL_PROCESSED_MESSAGES_PROCESSED_BY)
	};
	
	private Long changeNumber;
	private Timestamp timeStamp;
	private String processedBy;


	@Override
	public TableMapping<DBOProcessedMessage> getTableMapping() {
		return new TableMapping<DBOProcessedMessage>() {

			@Override
			public DBOProcessedMessage mapRow(ResultSet rs, int index)
					throws SQLException {
				DBOProcessedMessage change = new DBOProcessedMessage();
				change.setChangeNumber(rs.getLong(COL_PROCESSED_MESSAGES_CHANGE_NUM));
				change.setTimeStamp(rs.getTimestamp(COL_PROCESSED_MESSAGES_TIME_STAMP));
				change.setProcessedBy(rs.getString(COL_PROCESSED_MESSAGES_PROCESSED_BY));
				return change;
			}

			@Override
			public String getTableName() {
				return TABLE_PROCESSED_MESSAGES;
			}

			@Override
			public String getDDLFileName() {
				return DDL_PROCESSED_MESSAGES;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}

			@Override
			public Class<? extends DBOProcessedMessage> getDBOClass() {
				return DBOProcessedMessage.class;
			}
		};
	}
	public Long getChangeNumber() {
		return changeNumber;
	}

	public void setChangeNumber(Long changeNumber) {
		if (changeNumber == null) {
			throw new NullPointerException("changeNumber cannot be null");
		}
		this.changeNumber = changeNumber;
	}

	public Timestamp getTimeStamp() {
		return timeStamp;
	}

	public void setTimeStamp(Timestamp timeStamp) {
		if (timeStamp == null) {
			throw new NullPointerException("timeStamp cannot be null");
		}
		this.timeStamp = timeStamp;
	}
	
	public String getProcessedBy() {
		return processedBy;
	}
	
	public void setProcessedBy(String processedBy) {
		if (processedBy == null) {
			throw new NullPointerException("processedBy cannot be null");
		}
		this.processedBy = processedBy;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((changeNumber == null) ? 0 : changeNumber.hashCode());
		result = prime * result
				+ ((timeStamp == null) ? 0 : timeStamp.hashCode());
		result = prime * result
		+ ((processedBy == null) ? 0 : processedBy.hashCode());
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
		DBOProcessedMessage other = (DBOProcessedMessage) obj;
		if (changeNumber == null) {
			if (other.changeNumber != null)
				return false;
		} else if (!changeNumber.equals(other.changeNumber))
			return false;
		if (timeStamp == null) {
			if (other.timeStamp != null)
				return false;
		} else if (!timeStamp.equals(other.timeStamp))
			return false;
		if (processedBy == null) {
			if (other.processedBy != null)
				return false;
		} else if (!processedBy.equals(other.processedBy))
			return false;
		return true;
	}


}
