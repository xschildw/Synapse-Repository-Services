package org.sagebionetworks.audit.kinesis;

import org.sagebionetworks.kinesis.AwsKinesisLogRecord;
import org.sagebionetworks.repo.model.audit.NodeRecord;

import java.util.Objects;

public class NodeKinesisLogRecord implements AwsKinesisLogRecord {

	public static final String KINESIS_STREAM_NAME = "nodeSnapshots";

	private long timestamp;
	private String stack;
	private String instance;
	private long benefactorId;
	private long createdBy;
	private long createdOn;
	private long fileHandleId;
	private long modifiedBy;
	private long modifiedOn;
	private String name;
	private long nodeId;
	private long parentId;
	private long projectId;
	private String type;
	private long versionNumber;
	private Boolean isPublic;
	private Boolean isControlled;
	private Boolean isRestricted;

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

	public NodeKinesisLogRecord withNodeRecord(NodeRecord nodeRecord) {
		this.benefactorId = Long.parseLong(nodeRecord.getBenefactorId());
		this.createdBy = nodeRecord.getCreatedByPrincipalId();
		this.createdOn = nodeRecord.getCreatedOn().getTime();
		this.fileHandleId = Long.parseLong(nodeRecord.getFileHandleId());
		this.modifiedBy = nodeRecord.getModifiedByPrincipalId();
		this.modifiedOn = nodeRecord.getModifiedOn().getTime();
		this.name = nodeRecord.getName();
		this.nodeId = Long.parseLong(nodeRecord.getId());
		this.parentId = Long.parseLong(nodeRecord.getParentId());
		this.projectId = Long.parseLong(nodeRecord.getProjectId());
		this.type = nodeRecord.getNodeType().toString();
		this.versionNumber = nodeRecord.getVersionNumber();
		this.isPublic = nodeRecord.getIsPublic();
		this.isControlled = nodeRecord.getIsControlled();
		this.isRestricted = nodeRecord.getIsRestricted();
		return this;
	}

	public long getBenefactorId() { return benefactorId; }
	public void setBenefactorId(long benefactorId) {
		this.benefactorId = benefactorId;
	}

	public long getCreatedOn() { return createdOn; }
	public void setCreatedOn(long createdOn) {
		this.createdOn = createdOn;
	}

	public long getCreatedBy() { return createdBy; }
	public void setCreatedBy(long createdBy) {
		this.createdBy = createdBy;
	}

	public long getFileHandleId() { return fileHandleId; }
	public void setFileHandleId(long fileHandleId) {
		this.fileHandleId = fileHandleId;
	}

	public Boolean getIsControlled() { return isControlled; }
	public void setIsControlled(Boolean isControlled) {
		this.isControlled = isControlled;
	}

	public Boolean getIsPublic() { return isPublic; }
	public void setIsPublic(Boolean isPublic) {
		this.isPublic = isPublic;
	}

	public Boolean getIsRestricted() { return isRestricted; }
	public void setIsRestricteds(Boolean isRestricted) {
		this.isRestricted = isRestricted;
	}

	public long getModifiedBy() { return modifiedBy; }
	public void setModifiedBy(long modifiedBy) {
		this.modifiedBy = modifiedBy;
	}

	public long getModifiedOn() { return modifiedOn; }
	public void setModifiedOn(long modifiedOn) {
		this.modifiedOn = modifiedOn;
	}

	public String getName() { return name; }
	public void setName(String name) {
		this.name = name;
	}

	public long getNodeId() { return nodeId; }
	public void setNodeId(long nodeId) {
		this.nodeId = nodeId;
	}

	public long getParentId() { return parentId; }
	public void setParentId(long parentId) {
		this.parentId = parentId;
	}

	public long getProjectId() { return projectId; }
	public void setProjectId(long projectId) {
		this.projectId = projectId;
	}

	public String getType() { return type; }
	public void setType(String type) {
		this.type = type;
	}

	public long getVersionNumber() { return versionNumber; }
	public void setVersionNumber(long versionNumber) {
		this.versionNumber = versionNumber;
	}

	@Override
	public int hashCode() {
		return Objects.hash(timestamp, stack, instance, benefactorId, createdBy, createdOn,
				isControlled, isPublic, isRestricted, fileHandleId, modifiedBy,
				modifiedOn, name, parentId, projectId, type, versionNumber);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		NodeKinesisLogRecord that = (NodeKinesisLogRecord) o;
		return timestamp == that.timestamp &&
				Objects.equals(stack, that.stack) &&
				Objects.equals(instance, that.instance) &&
				Objects.equals(benefactorId, that.benefactorId) &&
				Objects.equals(createdBy, that.createdBy) &&
				Objects.equals(createdOn, that.createdOn) &&
				Objects.equals(fileHandleId, that.fileHandleId) &&
				Objects.equals(modifiedBy, that.modifiedBy) &&
				Objects.equals(modifiedOn, that.modifiedOn) &&
				Objects.equals(name, that.name) &&
				Objects.equals(nodeId, that.nodeId) &&
				Objects.equals(parentId, that.parentId) &&
				Objects.equals(projectId, that.projectId) &&
				Objects.equals(type, that.type) &&
				Objects.equals(versionNumber, that.versionNumber) &&
				Objects.equals(isControlled, that.isControlled) &&
				Objects.equals(isPublic, that.isPublic) &&
				Objects.equals(isRestricted, that.isRestricted);
	}

	@Override
	public String toString() {
		return "NodeKinesisLogRecord{" +
				"timestamp=" + timestamp +
				", stack='" + stack + '\'' +
				", instance='" + instance + '\'' +
				", benefactorId='" + benefactorId + "'" +
				", createdBy='" + createdBy + "'" +
				", createdOn='" + createdOn + "'" +
				", fileHandleId='" + fileHandleId + "'" +
				", modifiedBy='" + modifiedBy + "'" +
				", modifiedOn='" + modifiedOn + "'" +
				", name='" + name + "'" +
				", nodeId='" + nodeId + "'" +
				", parentId='" + parentId + "'" +
				", projectId='" + projectId + "'" +
				", type='" + type + "'" +
				", versioNumber='" + versionNumber + "'" +
				", isControlled='" + isControlled + "'" +
				", isPublic='" + isPublic + "'" +
				", isRestricted='" + isRestricted + "'" +
				'}';
	}
}
