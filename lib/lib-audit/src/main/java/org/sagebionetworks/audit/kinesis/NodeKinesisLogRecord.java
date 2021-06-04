package org.sagebionetworks.audit.kinesis;

import org.sagebionetworks.kinesis.AwsKinesisLogRecord;
import org.sagebionetworks.repo.model.audit.NodeRecord;

import java.util.Objects;

public class NodeKinesisLogRecord implements AwsKinesisLogRecord {

	public static final String KINESIS_STREAM_NAME = "nodeSnapshots";

	private long timestamp;
	private String stack;
	private String instance;
	private NodeRecord nodeRecord;

	public long getTimestamp() {
		return timestamp;
	}
	public NodeKinesisLogRecord withTimestamp(long timestamp) {
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

	public NodeRecord getNodeRecord() { return this.nodeRecord; }
	public NodeKinesisLogRecord withNodeRecord(NodeRecord nodeRecord) {
		this.nodeRecord = nodeRecord;
		return this;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		NodeKinesisLogRecord that = (NodeKinesisLogRecord) o;
		return timestamp == that.timestamp &&
				Objects.equals(stack, that.stack) &&
				Objects.equals(instance, that.instance) &&
				nodeRecord.equals(that.nodeRecord);
	}

	@Override
	public int hashCode() {
		return Objects.hash(timestamp, stack, instance, nodeRecord);
	}

	@Override
	public String toString() {
		return "NodeKinesisLogRecord{" +
				"timestamp=" + timestamp +
				", stack='" + stack + '\'' +
				", instance='" + instance + '\'' +
				", nodeRecord=" + nodeRecord +
				'}';
	}
}
