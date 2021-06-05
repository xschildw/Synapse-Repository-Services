package org.sagebionetworks.audit.kinesis;

import org.sagebionetworks.kinesis.AwsKinesisFirehoseLogger;
import org.sagebionetworks.kinesis.AwsKinesisLogRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ObjectRecordLoggerImpl implements ObjectRecordLogger {

	@Autowired
	AwsKinesisFirehoseLogger firehoseLogger;

	@Override
	public void saveBatch(String kinesisDataStreamSuffix, List<? extends AwsKinesisLogRecord> batch) {
		firehoseLogger.logBatch(kinesisDataStreamSuffix, batch);
	}
}