package org.sagebionetworks.audit.kinesis;

import org.sagebionetworks.audit.utils.ObjectRecordUtils;
import org.sagebionetworks.kinesis.AwsKinesisLogRecord;
import org.sagebionetworks.repo.model.audit.DeletedNode;

import java.util.Objects;

public class DeletedNodeKinesisLogRecord implements AwsKinesisLogRecord {
	public static final String KINESIS_STREAM_NAME = "deletedNodeSnapshots";

	private long timestamp;
	private String stack;
	private String instance;
	private Long id;

	public long getTimestamp() {
		return timestamp;
	}
	public DeletedNodeKinesisLogRecord withTimestamp(long timestamp) {
		this.timestamp = timestamp;
		return this;
	}

	@Override
	public String getStack() {
		return this.stack;
	}

	@Override
	public DeletedNodeKinesisLogRecord withStack(String stack) {
		this.stack =stack;
		return this;
	}

	@Override
	public String getInstance() {
		return this.instance;
	}
	@Override
	public DeletedNodeKinesisLogRecord withInstance(String instance) {
		this.instance = instance;
		return this;
	}

	public Long getId() { return this.id; }
	public void setId(Long id) {
		this.id = id;
	}

	public DeletedNodeKinesisLogRecord withDeletedNodeRecord(DeletedNode deletedNodeRecord) {
		this.id = ObjectRecordUtils.synapseIdToLong(deletedNodeRecord.getId());
		return this;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		DeletedNodeKinesisLogRecord that = (DeletedNodeKinesisLogRecord) o;
		return timestamp == that.timestamp &&
				Objects.equals(stack, that.stack) &&
				Objects.equals(instance, that.instance) &&
				Objects.equals(id, that.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(timestamp, stack, instance, id);
	}

	@Override
	public String toString() {
		return "DeletedNodeKinesisLogRecord{" +
				"timestamp=" + timestamp +
				", stack='" + stack + '\'' +
				", instance='" + instance + '\'' +
				", id=" + id +
				'}';
	}
}
