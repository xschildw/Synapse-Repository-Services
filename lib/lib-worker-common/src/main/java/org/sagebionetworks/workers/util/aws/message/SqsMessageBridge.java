package org.sagebionetworks.workers.util.aws.message;

import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;

/**
 * Converts an AWS SDK v2 SQS {@link Message} into the AWS SDK v1
 * {@link com.amazonaws.services.sqs.model.Message} still expected by the worker
 * runner contracts (e.g. {@link MessageDrivenRunner}).
 * <p>
 * Temporary: removed by PLFM-9749 (PR 3) once the runner contracts take the v2
 * {@link Message} directly.
 */
public class SqsMessageBridge {

	private SqsMessageBridge() {
	}

	/**
	 * @param message the message as received by the v2 client.
	 * @return an equivalent v1 message, or null when the given message is null.
	 */
	public static com.amazonaws.services.sqs.model.Message toV1(Message message) {
		if (message == null) {
			return null;
		}
		com.amazonaws.services.sqs.model.Message v1 = new com.amazonaws.services.sqs.model.Message()
				.withMessageId(message.messageId())
				.withReceiptHandle(message.receiptHandle())
				.withBody(message.body())
				.withMD5OfBody(message.md5OfBody())
				.withMD5OfMessageAttributes(message.md5OfMessageAttributes());
		if (message.hasAttributes()) {
			v1.setAttributes(new LinkedHashMap<>(message.attributesAsStrings()));
		}
		if (message.hasMessageAttributes()) {
			Map<String, com.amazonaws.services.sqs.model.MessageAttributeValue> v1Attributes = new LinkedHashMap<>();
			message.messageAttributes().forEach((name, value) -> v1Attributes.put(name, toV1(value)));
			v1.setMessageAttributes(v1Attributes);
		}
		return v1;
	}

	static com.amazonaws.services.sqs.model.MessageAttributeValue toV1(MessageAttributeValue value) {
		com.amazonaws.services.sqs.model.MessageAttributeValue v1 = new com.amazonaws.services.sqs.model.MessageAttributeValue()
				.withDataType(value.dataType())
				.withStringValue(value.stringValue())
				.withBinaryValue(toByteBuffer(value.binaryValue()));
		if (value.hasStringListValues()) {
			v1.setStringListValues(value.stringListValues());
		}
		if (value.hasBinaryListValues()) {
			v1.setBinaryListValues(toByteBuffers(value.binaryListValues()));
		}
		return v1;
	}

	private static List<ByteBuffer> toByteBuffers(List<SdkBytes> bytes) {
		return bytes.stream().map(SqsMessageBridge::toByteBuffer).collect(Collectors.toList());
	}

	private static ByteBuffer toByteBuffer(SdkBytes bytes) {
		// v2 hands out read-only buffers; v1 callers may expect a backing array.
		return bytes == null ? null : ByteBuffer.wrap(bytes.asByteArray());
	}
}
