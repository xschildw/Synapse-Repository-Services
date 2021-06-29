package org.sagebionetworks.audit.kinesis;

import org.sagebionetworks.kinesis.AwsKinesisLogRecord;
import org.sagebionetworks.repo.model.Team;

import java.util.Objects;

public class TeamKinesisLogRecord implements AwsKinesisLogRecord {
	public static final String KINESIS_STREAM_NAME = "teamSnapshots";

	private long timestamp;
	private String stack;
	private String instance;
	private Team team;

	public long getTimestamp() {
		return timestamp;
	}
	public TeamKinesisLogRecord withTimestamp(long timestamp) {
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

	public Team getTeam() { return this.team; }
	public TeamKinesisLogRecord withTeam(Team team) {
		this.team = team;
		return this;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		TeamKinesisLogRecord that = (TeamKinesisLogRecord) o;
		return timestamp == that.timestamp &&
				Objects.equals(stack, that.stack) &&
				Objects.equals(instance, that.instance) &&
				team.equals(that.team);
	}

	@Override
	public int hashCode() {
		return Objects.hash(timestamp, stack, instance, team);
	}

	@Override
	public String toString() {
		return "TeamKinesisLogRecord{" +
				"timestamp=" + timestamp +
				", stack='" + stack + '\'' +
				", instance='" + instance + '\'' +
				", team=" + team +
				'}';
	}
}
