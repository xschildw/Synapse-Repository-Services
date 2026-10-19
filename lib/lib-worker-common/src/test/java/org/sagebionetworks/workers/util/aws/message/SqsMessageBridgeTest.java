package org.sagebionetworks.workers.util.aws.message;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.MessageSystemAttributeName;

public class SqsMessageBridgeTest {

	@Test
	public void testToV1WithAllFields() {
		Message message = Message.builder()
				.messageId("messageId")
				.receiptHandle("receiptHandle")
				.body("{\"objectId\":\"123\"}")
				.md5OfBody("md5OfBody")
				.md5OfMessageAttributes("md5OfMessageAttributes")
				.attributes(Map.of(
						MessageSystemAttributeName.APPROXIMATE_RECEIVE_COUNT, "3",
						MessageSystemAttributeName.SENT_TIMESTAMP, "1700000000000"))
				.messageAttributes(Map.of(
						"stringAttribute", MessageAttributeValue.builder().dataType("String").stringValue("stringValue").build(),
						"numberAttribute", MessageAttributeValue.builder().dataType("Number").stringValue("42").build(),
						"binaryAttribute", MessageAttributeValue.builder().dataType("Binary").binaryValue(bytes("binaryValue")).build(),
						"stringListAttribute", MessageAttributeValue.builder().dataType("String").stringListValues("one", "two").build(),
						"binaryListAttribute", MessageAttributeValue.builder().dataType("Binary").binaryListValues(bytes("three"), bytes("four")).build()))
				.build();

		com.amazonaws.services.sqs.model.Message expected = new com.amazonaws.services.sqs.model.Message()
				.withMessageId("messageId")
				.withReceiptHandle("receiptHandle")
				.withBody("{\"objectId\":\"123\"}")
				.withMD5OfBody("md5OfBody")
				.withMD5OfMessageAttributes("md5OfMessageAttributes")
				.withAttributes(Map.of(
						"ApproximateReceiveCount", "3",
						"SentTimestamp", "1700000000000"))
				.withMessageAttributes(Map.of(
						"stringAttribute", new com.amazonaws.services.sqs.model.MessageAttributeValue().withDataType("String").withStringValue("stringValue"),
						"numberAttribute", new com.amazonaws.services.sqs.model.MessageAttributeValue().withDataType("Number").withStringValue("42"),
						"binaryAttribute", new com.amazonaws.services.sqs.model.MessageAttributeValue().withDataType("Binary").withBinaryValue(buffer("binaryValue")),
						"stringListAttribute", new com.amazonaws.services.sqs.model.MessageAttributeValue().withDataType("String").withStringListValues("one", "two"),
						"binaryListAttribute", new com.amazonaws.services.sqs.model.MessageAttributeValue().withDataType("Binary").withBinaryListValues(buffer("three"), buffer("four"))));

		// call under test
		com.amazonaws.services.sqs.model.Message result = SqsMessageBridge.toV1(message);

		assertEquals(expected, result);
		assertEquals("3", result.getAttributes().get("ApproximateReceiveCount"));
	}

	@Test
	public void testToV1WithBodyOnly() {
		Message message = Message.builder()
				.messageId("messageId")
				.receiptHandle("receiptHandle")
				.body("body")
				.build();

		com.amazonaws.services.sqs.model.Message expected = new com.amazonaws.services.sqs.model.Message()
				.withMessageId("messageId")
				.withReceiptHandle("receiptHandle")
				.withBody("body");

		// call under test
		com.amazonaws.services.sqs.model.Message result = SqsMessageBridge.toV1(message);

		assertEquals(expected, result);
		assertTrue(result.getAttributes().isEmpty());
		assertTrue(result.getMessageAttributes().isEmpty());
	}

	@Test
	public void testToV1WithNullMessage() {
		// call under test
		assertNull(SqsMessageBridge.toV1((Message) null));
	}

	@Test
	public void testToV1WithBinaryValueIsWritable() {
		MessageAttributeValue value = MessageAttributeValue.builder()
				.dataType("Binary")
				.binaryValue(bytes("binaryValue"))
				.binaryListValues(bytes("listValue"))
				.build();

		// call under test
		com.amazonaws.services.sqs.model.MessageAttributeValue result = SqsMessageBridge.toV1(value);

		assertTrue(result.getBinaryValue().hasArray());
		assertFalse(result.getBinaryValue().isReadOnly());
		assertTrue(result.getBinaryListValues().get(0).hasArray());
		assertEquals(List.of(buffer("listValue")), result.getBinaryListValues());
	}

	private static SdkBytes bytes(String value) {
		return SdkBytes.fromString(value, StandardCharsets.UTF_8);
	}

	private static ByteBuffer buffer(String value) {
		return ByteBuffer.wrap(value.getBytes(StandardCharsets.UTF_8));
	}
}
