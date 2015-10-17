package org.sagebionetworks.file.worker;

import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.doNothing;

import java.io.EOFException;
import java.util.List;

import javax.imageio.IIOException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.cloudwatch.WorkerLogger;
import org.sagebionetworks.repo.manager.file.preview.LocalPreviewManager;
import org.sagebionetworks.repo.manager.file.preview.PreviewManager;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.file.ExternalFileHandle;
import org.sagebionetworks.repo.model.file.PreviewFileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.TemporarilyUnavailableException;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.sagebionetworks.workers.util.progress.ProgressCallback;
import org.springframework.test.util.ReflectionTestUtils;

import com.amazonaws.services.sqs.model.Message;

public class PreviewWorkerTest {
	
	ProgressCallback<ChangeMessage> mockProgressCallback;
	PreviewManager mockPreviewManager;
	ChangeMessage change;
	WorkerLogger mockWorkerLogger;
	PreviewWorker worker;
	
	@Before
	public void before(){
		mockPreviewManager = Mockito.mock(PreviewManager.class);
		mockProgressCallback = Mockito.mock(ProgressCallback.class);
		change = new ChangeMessage();
		change.setObjectType(ObjectType.FILE);
		change.setObjectId("123");
		change.setChangeType(ChangeType.CREATE);
		mockWorkerLogger = Mockito.mock(WorkerLogger.class);
		worker = new PreviewWorker();
		ReflectionTestUtils.setField(worker, "previewManager", mockPreviewManager);
		ReflectionTestUtils.setField(worker, "workerLogger", mockWorkerLogger);
	}

	@Test
	public void testNotFound() throws Exception{
		// When a file is not found the message must be returned so it can be removed from the queue
		when(mockPreviewManager.getFileMetadata(change.getObjectId())).thenThrow(new NotFoundException());
		// Fire!
		worker.run(mockProgressCallback, change);
		verify(mockWorkerLogger, never()).logWorkerFailure(eq(PreviewWorker.class), eq(change), any(NotFoundException.class), eq(false));
	}
	
	@Test
	public void testPreviewMessage() throws Exception{
		// We do not create previews for previews.
		PreviewFileHandle pfm = new PreviewFileHandle();
		when(mockPreviewManager.getFileMetadata(change.getObjectId())).thenReturn(pfm);
		// Fire!
		worker.run(mockProgressCallback, change);
		verify(mockWorkerLogger, never()).logWorkerFailure(eq(PreviewWorker.class), eq(change), any(NotFoundException.class), eq(false));
	}
	
	@Test
	public void testNonFileMessage() throws Exception{
		// Non-file messages should be ignored and marked as processed.
		change = new ChangeMessage();
		change.setObjectType(ObjectType.ENTITY);
		change.setObjectId("123");
		// Fire!
		worker.run(mockProgressCallback, change);
		verify(mockWorkerLogger, never()).logWorkerFailure(eq(PreviewWorker.class), eq(change), any(NotFoundException.class), eq(false));
	}
	
	@Test
	public void testExternalFileMessage() throws Exception{
		// We do not create previews for previews.
		ExternalFileHandle meta = new ExternalFileHandle();
		when(mockPreviewManager.getFileMetadata(change.getObjectId())).thenReturn(meta);
		// Fire!
		worker.run(mockProgressCallback, change);
		verify(mockWorkerLogger, never()).logWorkerFailure(eq(PreviewWorker.class), eq(change), any(NotFoundException.class), eq(false));
	}
	
	@Test
	public void testS3FileMetadataMessage() throws Exception{
		// We do not create previews for previews.
		S3FileHandle meta = new S3FileHandle();
		when(mockPreviewManager.getFileMetadata(change.getObjectId())).thenReturn(meta);
		// Fire!
		worker.run(mockProgressCallback, change);
		verify(mockWorkerLogger, never()).logWorkerFailure(eq(PreviewWorker.class), eq(change), any(NotFoundException.class), eq(false));
	}
	
	@Test
	public void testUpdateMessage() throws Exception{
		change.setChangeType(ChangeType.UPDATE);
		S3FileHandle meta = new S3FileHandle();
		when(mockPreviewManager.getFileMetadata(change.getObjectId())).thenReturn(meta);
		// Fire!
		worker.run(mockProgressCallback, change);
		// We should generate 
		verify(mockPreviewManager).handle(any(S3FileHandle.class));
		verify(mockWorkerLogger, never()).logWorkerFailure(eq(PreviewWorker.class), eq(change), any(NotFoundException.class), eq(false));
	}
	
	@Test
	public void testTemporarilyUnavailable() throws Exception{
		// When the preview manager throws a TemporarilyUnavailableException
		// that means it could not process this message right now.  Therefore,
		// the message should not be returned, so it will stay on the queue.
		S3FileHandle meta = new S3FileHandle();
		when(mockPreviewManager.getFileMetadata(change.getObjectId())).thenReturn(meta);
		TemporarilyUnavailableException expectedException = new TemporarilyUnavailableException();
		doThrow(expectedException).when(mockPreviewManager).handle(meta);
		try {
			// Fire!
			worker.run(mockProgressCallback, change);
			fail("Should have thrown an exception");
		} catch (RecoverableMessageException e) {
			// expected
		}
		verify(mockWorkerLogger).logWorkerFailure(PreviewWorker.class, change, expectedException, true);
	}
	
	@Test
	public void testUnknownError() throws Exception{
		// If we do not know what type of error occurred, then we assume
		// that we will be able to recover from it and therefore, the message
		// should not be returned as processed.
		S3FileHandle meta = new S3FileHandle();
		when(mockPreviewManager.getFileMetadata(change.getObjectId())).thenReturn(meta);
		Exception expectedException = new Exception();
		doThrow(expectedException).when(mockPreviewManager).handle(meta);
		// Fire!
		worker.run(mockProgressCallback, change);
		verify(mockWorkerLogger).logWorkerFailure(PreviewWorker.class, change, expectedException, false);
	}
	
	@Test
	public void testEOFError() throws Exception {
		S3FileHandle meta = new S3FileHandle();
		when(mockPreviewManager.getFileMetadata(change.getObjectId())).thenReturn(meta);
		Exception expectedException = new RuntimeException();
		expectedException.initCause(new EOFException());
		doThrow(expectedException).when(mockPreviewManager).handle(meta);
		// Fire!
		worker.run(mockProgressCallback, change);
		verify(mockWorkerLogger).logWorkerFailure(PreviewWorker.class, change, expectedException, false);
	}
	
	@Test
	public void testIllegalArgumentException() throws Exception{
		// We cannot recover from this type of exception so the message should be returned.
		S3FileHandle meta = new S3FileHandle();
		when(mockPreviewManager.getFileMetadata(change.getObjectId())).thenReturn(meta);
		IllegalArgumentException expectedException = new IllegalArgumentException();
		doThrow(expectedException).when(mockPreviewManager).handle(meta);
		// Fire!
		worker.run(mockProgressCallback, change);
		verify(mockWorkerLogger).logWorkerFailure(PreviewWorker.class, change, expectedException, false);
	}
	
	@Test
	public void testErrorReadingPNG() throws Exception{
		// We cannot recover from this type of exception so the message should be returned.
		S3FileHandle meta = new S3FileHandle();
		when(mockPreviewManager.getFileMetadata(change.getObjectId())).thenReturn(meta);
		IIOException causeException = new javax.imageio.IIOException("Error reading PNG image data");
		RuntimeException expectedException = new RuntimeException(causeException);
		doThrow(expectedException).when(mockPreviewManager).handle(meta);
		// Fire!
		worker.run(mockProgressCallback, change);
		verify(mockWorkerLogger).logWorkerFailure(PreviewWorker.class, change, expectedException, false);
	}
	
	@Test
	public void testEmptyFile() throws Exception{
		// We cannot recover from this type of exception so the message should be returned.
		S3FileHandle meta = new S3FileHandle();
		meta.setContentSize(0L);
		when(mockPreviewManager.getFileMetadata(change.getObjectId())).thenReturn(meta);
		doNothing().when(mockPreviewManager).handle(meta);
		// Fire!
		worker.run(mockProgressCallback, change);
		// We should generate 
		verify(mockPreviewManager).handle(any(S3FileHandle.class));
		verify(mockWorkerLogger, never()).logWorkerFailure(eq(PreviewWorker.class), eq(change), any(NotFoundException.class), anyBoolean());
	}
	
	@Test
	public void testIgnoreDeleteMessage() throws Exception{
		// Update messages should be ignored.
		change.setChangeType(ChangeType.DELETE);
		S3FileHandle meta = new S3FileHandle();
		when(mockPreviewManager.getFileMetadata(change.getObjectId())).thenReturn(meta);
		// Fire!
		worker.run(mockProgressCallback, change);
		// We should not generate a 
		verify(mockPreviewManager, never()).handle(any(S3FileHandle.class));
		verify(mockWorkerLogger, never()).logWorkerFailure(eq(PreviewWorker.class), eq(change), any(NotFoundException.class), eq(false));
	}
	
	@Test
	public void testIgnoreDeleteessage() throws Exception{
		// delete messages should be ignored.
		change.setChangeType(ChangeType.DELETE);
		// Fire!
		worker.run(mockProgressCallback, change);
		// We should not generate a 
		verify(mockPreviewManager, never()).handle(any(S3FileHandle.class));
		verify(mockWorkerLogger, never()).logWorkerFailure(eq(PreviewWorker.class), eq(change), any(NotFoundException.class), eq(false));
	}
	
	/**
	 * Helper to validate that the passed message was processed and on the resulting list.
	 * @param processedList
	 * @param message
	 */
	public boolean isMessageOnList(List<Message> processedList, Message message){
		for(Message m: processedList){
			if(m.equals(message)){
				return true;
			}
		}
		return false;
	}
}
