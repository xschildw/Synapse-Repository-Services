package org.sagebionetworks.audit.kinesis;

import org.sagebionetworks.kinesis.AwsKinesisLogRecord;

import java.util.List;

public interface ObjectRecordLogger {
	public void saveBatch(String kinesisDataStreamSuffix, List<? extends AwsKinesisLogRecord> batch);
}