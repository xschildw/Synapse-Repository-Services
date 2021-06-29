package org.sagebionetworks.audit.kinesis;

import org.sagebionetworks.kinesis.AwsKinesisLogRecord;
import org.sagebionetworks.repo.model.UserGroup;

import java.util.Objects;

public class UserGroupKinesisLogRecord implements AwsKinesisLogRecord {
	public static final String KINESIS_STREAM_NAME = "userGroupSnapshots";

	private long timestamp;
	private String stack;
	private String instance;
	private UserGroup userGroup;

	public long getTimestamp() {
		return timestamp;
	}
	public UserGroupKinesisLogRecord withTimestamp(long timestamp) {
		this.timestamp = timestamp;
		return this;
	}

	@Override
	public String getStack() {
		return this.stack;
	}
	@Override
	public AwsKinesisLogRecord withStack(String stack) {
		this.stack =stack;
		return this;
	}

	@Override
	public String getInstance() {
		return this.instance;
	}
	@Override
	public AwsKinesisLogRecord withInstance(String instance) {
		this.instance = instance;
		return this;
	}

	public UserGroup getUserGroup() { return this.userGroup; }
	public UserGroupKinesisLogRecord withUserGroup(UserGroup userGroup) {
		this.userGroup = userGroup;
		return this;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		UserGroupKinesisLogRecord that = (UserGroupKinesisLogRecord) o;
		return timestamp == that.timestamp &&
				Objects.equals(stack, that.stack) &&
				Objects.equals(instance, that.instance) &&
				userGroup.equals(that.userGroup);
	}

	@Override
	public int hashCode() {
		return Objects.hash(timestamp, stack, instance, userGroup);
	}

	@Override
	public String toString() {
		return "UserGroupKinesisLogRecord{" +
				"timestamp=" + timestamp +
				", stack='" + stack + '\'' +
				", instance='" + instance + '\'' +
				", userGroup=" + userGroup +
				'}';
	}
}
