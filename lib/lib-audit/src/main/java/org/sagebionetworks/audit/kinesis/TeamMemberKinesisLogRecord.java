package org.sagebionetworks.audit.kinesis;

import org.sagebionetworks.kinesis.AwsKinesisLogRecord;
import org.sagebionetworks.repo.model.TeamMember;

import java.util.Objects;

public class TeamMemberKinesisLogRecord implements AwsKinesisLogRecord {
	public static final String KINESIS_STREAM_NAME = "teamMemberSnapshots";

	private long timestamp;
	private String stack;
	private String instance;
	private TeamMember teamMember;

	public long getTimestamp() {
		return timestamp;
	}
	public TeamMemberKinesisLogRecord withTimestamp(long timestamp) {
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

	public TeamMember getTeamMember() { return this.teamMember; }
	public TeamMemberKinesisLogRecord withTeam(TeamMember teamMember) {
		this.teamMember = teamMember;
		return this;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		TeamMemberKinesisLogRecord that = (TeamMemberKinesisLogRecord) o;
		return timestamp == that.timestamp &&
				Objects.equals(stack, that.stack) &&
				Objects.equals(instance, that.instance) &&
				teamMember.equals(that.teamMember);
	}

	@Override
	public int hashCode() {
		return Objects.hash(timestamp, stack, instance, teamMember);
	}

	@Override
	public String toString() {
		return "TeamMemberKinesisLogRecord{" +
				"timestamp=" + timestamp +
				", stack='" + stack + '\'' +
				", instance='" + instance + '\'' +
				", teamMember=" + teamMember +
				'}';
	}
}
