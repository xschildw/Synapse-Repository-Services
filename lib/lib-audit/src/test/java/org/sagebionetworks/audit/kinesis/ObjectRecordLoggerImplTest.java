package org.sagebionetworks.audit.kinesis;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.kinesis.AwsKinesisFirehoseLogger;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.audit.AclRecord;
import org.sagebionetworks.repo.model.audit.DeletedNode;
import org.sagebionetworks.repo.model.audit.NodeRecord;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ObjectRecordLoggerImplTest {

	@Mock
	private AwsKinesisFirehoseLogger mockAwsKinesisFirehoseLogger;

	private ObjectRecordLoggerImpl objectRecordLogger;

	@BeforeEach
	void setUp() {
		objectRecordLogger = new ObjectRecordLoggerImpl(mockAwsKinesisFirehoseLogger);
	}

	@AfterEach
	void tearDown() {
	}

	@Test
	void saveBatchAclRecord() {
		AclRecord aclRec = new AclRecord();
		aclRec.setOwnerType(ObjectType.ENTITY);
		aclRec.setId("987654");
		AclKinesisLogRecord aclKinesisLogRecord = new AclKinesisLogRecord().withTimestamp(123456L).withAclRecord(aclRec);
		List<AclKinesisLogRecord> aclKinesisLogRecords = Collections.singletonList(aclKinesisLogRecord);

		// Call under test
		objectRecordLogger.saveBatch(AclKinesisLogRecord.KINESIS_STREAM_NAME, aclKinesisLogRecords);

		verify(mockAwsKinesisFirehoseLogger).logBatch(eq(AclKinesisLogRecord.KINESIS_STREAM_NAME), eq(aclKinesisLogRecords));

	}

	@Test
	void saveBatchNodeRecord() {
		NodeRecord nodeRec = new NodeRecord();
		nodeRec.setBenefactorId("syn12345");
		nodeRec.setCreatedByPrincipalId(123L);
		nodeRec.setCreatedOn(new Date());
		nodeRec.setModifiedOn(new Date());
		nodeRec.setModifiedByPrincipalId(123L);
		nodeRec.setParentId("syn12345");
		nodeRec.setProjectId("syn12345");
		nodeRec.setVersionNumber(1L);
		nodeRec.setNodeType(EntityType.file);
		nodeRec.setId("987654");
		nodeRec.setFileHandleId("789012");
		NodeKinesisLogRecord nodeKinesisLogRecord = new NodeKinesisLogRecord().withTimestamp(123456L).withNodeRecord(nodeRec);
		List<NodeKinesisLogRecord> nodeKinesisLogRecords = Collections.singletonList(nodeKinesisLogRecord);

		// Call under test
		objectRecordLogger.saveBatch(NodeKinesisLogRecord.KINESIS_STREAM_NAME, nodeKinesisLogRecords);

		verify(mockAwsKinesisFirehoseLogger).logBatch(eq(NodeKinesisLogRecord.KINESIS_STREAM_NAME), eq(nodeKinesisLogRecords));

	}

	@Test
	void saveBatchDeletedNodeRecord() {
		DeletedNode deletedNode = new DeletedNode();
		deletedNode.setId("syn987654");
		DeletedNodeKinesisLogRecord deletedNodeKinesisLogRecord = new DeletedNodeKinesisLogRecord().withTimestamp(123456L).withDeletedNodeRecord(deletedNode);
		List<DeletedNodeKinesisLogRecord> deletedNodeKinesisLogRecords = Collections.singletonList(deletedNodeKinesisLogRecord);

		// Call under test
		objectRecordLogger.saveBatch(DeletedNodeKinesisLogRecord.KINESIS_STREAM_NAME, deletedNodeKinesisLogRecords);

		verify(mockAwsKinesisFirehoseLogger).logBatch(eq(DeletedNodeKinesisLogRecord.KINESIS_STREAM_NAME), eq(deletedNodeKinesisLogRecords));

	}

}