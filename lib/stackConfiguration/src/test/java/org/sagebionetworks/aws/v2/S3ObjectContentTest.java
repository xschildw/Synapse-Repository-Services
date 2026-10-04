package org.sagebionetworks.aws.v2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.io.InputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

@ExtendWith(MockitoExtension.class)
public class S3ObjectContentTest {

	@Mock
	private ResponseInputStream<GetObjectResponse> mockResponseStream;

	@Mock
	private InputStream mockInputStream;

	private final S3ObjectInfo info = S3ObjectInfo.builder().withContentType("text/plain").build();

	@Test
	public void testAbortWithAbortableStream() throws Exception {
		S3ObjectContent content = new S3ObjectContent(info, mockResponseStream);

		// call under test
		content.abort();

		verify(mockResponseStream).abort();
		verify(mockResponseStream, never()).close();
	}

	@Test
	public void testAbortWithPlainStream() throws Exception {
		S3ObjectContent content = new S3ObjectContent(info, mockInputStream);

		// call under test
		content.abort();

		verify(mockInputStream).close();
	}

	@Test
	public void testClose() throws Exception {
		S3ObjectContent content = new S3ObjectContent(info, mockResponseStream);

		// call under test
		content.close();

		verify(mockResponseStream).close();
		verify(mockResponseStream, never()).abort();
	}

	@Test
	public void testConstructorWithNullInfo() {
		// call under test
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
				() -> new S3ObjectContent(null, mockInputStream));

		assertEquals("info is required.", e.getMessage());
	}

	@Test
	public void testConstructorWithNullContent() {
		// call under test
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
				() -> new S3ObjectContent(info, null));

		assertEquals("content is required.", e.getMessage());
	}

}
