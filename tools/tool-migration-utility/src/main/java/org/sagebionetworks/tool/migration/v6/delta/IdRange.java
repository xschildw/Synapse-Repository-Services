package org.sagebionetworks.tool.migration.v6.delta;

public class IdRange {
	
	private long minId;
	private long maxId;
	
	public IdRange(long min, long max) {
		this.setMinId(min);
		this.setMaxId(max);
	}

	public long getMinId() {
		return minId;
	}

	public void setMinId(long minId) {
		this.minId = minId;
	}

	public long getMaxId() {
		return maxId;
	}

	public void setMaxId(long maxId) {
		this.maxId = maxId;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("[");
		builder.append(this.minId);
		builder.append(", ");
		builder.append(this.maxId);
		builder.append("]");
		return builder.toString();
	}
}
