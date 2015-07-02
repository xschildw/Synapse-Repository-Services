package org.sagebionetworks.table.worker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.asynchronous.workers.sqs.MessageUtils;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.asynch.AsynchJobStatusManager;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.manager.table.TableRowManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.asynch.AsynchronousResponseBody;
import org.sagebionetworks.repo.model.dbo.dao.table.TableModelTestUtils;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.CsvTableDescriptor;
import org.sagebionetworks.repo.model.table.UploadToTableRequest;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.csv.CsvNullReader;
import org.sagebionetworks.workers.util.progress.ProgressCallback;
import org.springframework.test.util.ReflectionTestUtils;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.util.StringInputStream;

/**
 * Unit test for the TableCSVAppenderWorker.
 * 
 * @author John
 *
 */
public class TableCSVAppenderWorkerTest {
	
	ProgressCallback<Message> mockProgressCallback;
	AsynchJobStatusManager mockAasynchJobStatusManager;
	TableRowManager mockTableRowManager;
	FileHandleManager  mockFileHandleManger;
	UserManager mockUserManager;
	AmazonS3Client mockS3Client;
	AsynchronousJobStatus status;
	TableCSVAppenderWorker worker;
	S3FileHandle fileHandle;
	UserInfo user;
	List<ColumnModel> tableSchema;
	ObjectMetadata fileMetadata;
	S3Object s3Object;
	String csvString;
	
	@Before
	public void before() throws Exception {
		mockProgressCallback = Mockito.mock(ProgressCallback.class);
		mockAasynchJobStatusManager = Mockito.mock(AsynchJobStatusManager.class);
		mockTableRowManager = Mockito.mock(TableRowManager.class);
		mockFileHandleManger = Mockito.mock(FileHandleManager.class);
		mockUserManager = Mockito.mock(UserManager.class);
		mockS3Client = Mockito.mock(AmazonS3Client.class);
		// User
		user = new UserInfo(false);
		user.setId(999L);
		// Message
		status = new AsynchronousJobStatus();
		status.setChangedOn(new Date());
		status.setJobId("123");
		status.setStartedByUserId(user.getId());
		UploadToTableRequest body = new UploadToTableRequest();
		body.setTableId("syn456");
		body.setUploadFileHandleId("789");
		status.setRequestBody(body);

		// FileHandle
		fileHandle = new S3FileHandle();
		fileHandle.setId(body.getUploadFileHandleId());
		fileHandle.setBucketName("someBucket");
		fileHandle.setKey("someKey");
		fileHandle.setContentType("text/csv");
		// meta
		fileMetadata = new ObjectMetadata();
		// Object 
		s3Object = new S3Object();
		// schema
		tableSchema = TableModelTestUtils.createColumsWithNames("a","b","c");
		worker = new TableCSVAppenderWorker();
		ReflectionTestUtils.setField(worker, "asynchJobStatusManager", mockAasynchJobStatusManager);
		ReflectionTestUtils.setField(worker, "tableRowManager", mockTableRowManager);
		ReflectionTestUtils.setField(worker, "fileHandleManager", mockFileHandleManger);
		ReflectionTestUtils.setField(worker, "userManger", mockUserManager);
		ReflectionTestUtils.setField(worker, "s3Client", mockS3Client);
		// Create the CSV
		List<String[]> input = new ArrayList<String[]>(3);
		input.add(new String[] { "a", "b", "c" });
		input.add(new String[] { "CCC", null, "false" });
		input.add(new String[] { "FFF", "4", "true" });
		csvString = TableModelTestUtils.createCSVString(input);
		StringInputStream csvStream = new StringInputStream(csvString);
		s3Object.setObjectContent(new S3ObjectInputStream(csvStream, null));

		when(mockUserManager.getUserInfo(user.getId())).thenReturn(user);
		when(mockFileHandleManger.getRawFileHandle(user, body.getUploadFileHandleId())).thenReturn(fileHandle);
		when(mockS3Client.getObjectMetadata(fileHandle.getBucketName(), fileHandle.getKey())).thenReturn(fileMetadata);
		when(mockS3Client.getObject(fileHandle.getBucketName(), fileHandle.getKey())).thenReturn(s3Object);
		when(mockTableRowManager.getColumnModelsForTable(body.getTableId())).thenReturn(tableSchema);
	}

	@Test
	public void testHappyCase() throws Exception {
		Message message = MessageUtils.buildMessage(status);
		// call under test
		worker.run(mockProgressCallback, message);
		// Status for this job should be marked as complete.
		verify(mockAasynchJobStatusManager, times(1)).setComplete(anyString(), any(AsynchronousResponseBody.class));
	}
	
	@Test
	public void testFailure() throws Exception {
		when(mockUserManager.getUserInfo(user.getId())).thenThrow(new NotFoundException("Cannot find the user"));
		Message message = MessageUtils.buildMessage(status);
		// call under test
		worker.run(mockProgressCallback, message);
		// Status for this job should not be set to complete
		verify(mockAasynchJobStatusManager, times(0)).setComplete(anyString(), any(AsynchronousResponseBody.class));
		// The job just be set to failed.
		verify(mockAasynchJobStatusManager, times(1)).setJobFailed(anyString(), any(Throwable.class));
	}
	
	@Test
	public void testCreateCSVReaderAllDefaults(){
		// an empty body should result in all of the default values.
		UploadToTableRequest body = new UploadToTableRequest();
		StringReader reader = new StringReader("1,2,3");
		CsvNullReader csvReader = CSVUtils.createCSVReader(reader, body.getCsvTableDescriptor(), body.getLinesToSkip());
		assertNotNull(csvReader);
		assertEquals(CsvNullReader.DEFAULT_SEPARATOR, csvReader.getSeparator());
		assertEquals(CsvNullReader.DEFAULT_ESCAPE_CHARACTER, csvReader.getEscape());
		assertEquals(CsvNullReader.DEFAULT_QUOTE_CHARACTER, csvReader.getQuotechar());
		assertEquals(CsvNullReader.DEFAULT_SKIP_LINES, csvReader.getSkipLines());
	}
	
	@Test
	public void testCreateCSVReaderTabSeperator(){
		// an empty body should result in all of the default values.
		UploadToTableRequest body = new UploadToTableRequest();
		body.setCsvTableDescriptor(new CsvTableDescriptor());
		body.getCsvTableDescriptor().setSeparator("\t");
		StringReader reader = new StringReader("1,2,3");
		CsvNullReader csvReader = CSVUtils.createCSVReader(reader, body.getCsvTableDescriptor(), body.getLinesToSkip());
		assertNotNull(csvReader);
		assertEquals('\t', csvReader.getSeparator());
		assertEquals(CsvNullReader.DEFAULT_ESCAPE_CHARACTER, csvReader.getEscape());
		assertEquals(CsvNullReader.DEFAULT_QUOTE_CHARACTER, csvReader.getQuotechar());
		assertEquals(CsvNullReader.DEFAULT_SKIP_LINES, csvReader.getSkipLines());
	}
	
	@Test
	public void testCreateCSVReaderEscapse(){
		// an empty body should result in all of the default values.
		UploadToTableRequest body = new UploadToTableRequest();
		body.setCsvTableDescriptor(new CsvTableDescriptor());
		body.getCsvTableDescriptor().setEscapeCharacter("\n");
		StringReader reader = new StringReader("1,2,3");
		CsvNullReader csvReader = CSVUtils.createCSVReader(reader, body.getCsvTableDescriptor(), body.getLinesToSkip());
		assertNotNull(csvReader);
		assertEquals(CsvNullReader.DEFAULT_SEPARATOR, csvReader.getSeparator());
		assertEquals('\n', csvReader.getEscape());
		assertEquals(CsvNullReader.DEFAULT_QUOTE_CHARACTER, csvReader.getQuotechar());
		assertEquals(CsvNullReader.DEFAULT_SKIP_LINES, csvReader.getSkipLines());
	}
	
	@Test
	public void testCreateCSVReaderQuote(){
		// an empty body should result in all of the default values.
		UploadToTableRequest body = new UploadToTableRequest();
		body.setCsvTableDescriptor(new CsvTableDescriptor());
		body.getCsvTableDescriptor().setQuoteCharacter("'");
		StringReader reader = new StringReader("1,2,3");
		CsvNullReader csvReader = CSVUtils.createCSVReader(reader, body.getCsvTableDescriptor(), body.getLinesToSkip());
		assertNotNull(csvReader);
		assertEquals(CsvNullReader.DEFAULT_SEPARATOR, csvReader.getSeparator());
		assertEquals(CsvNullReader.DEFAULT_ESCAPE_CHARACTER, csvReader.getEscape());
		assertEquals('\'', csvReader.getQuotechar());
		assertEquals(CsvNullReader.DEFAULT_SKIP_LINES, csvReader.getSkipLines());
	}
	
	@Test
	public void testCreateCSVReaderSkipLine(){
		// an empty body should result in all of the default values.
		UploadToTableRequest body = new UploadToTableRequest();
		body.setLinesToSkip(101L);
		StringReader reader = new StringReader("1,2,3");
		CsvNullReader csvReader = CSVUtils.createCSVReader(reader, body.getCsvTableDescriptor(), body.getLinesToSkip());
		assertNotNull(csvReader);
		assertEquals(CsvNullReader.DEFAULT_SEPARATOR, csvReader.getSeparator());
		assertEquals(CsvNullReader.DEFAULT_ESCAPE_CHARACTER, csvReader.getEscape());
		assertEquals(CsvNullReader.DEFAULT_QUOTE_CHARACTER, csvReader.getQuotechar());
		assertEquals(101, csvReader.getSkipLines());
	}
	
	@Test
	public void testCreateCSVReaderAllOverride(){
		// an empty body should result in all of the default values.
		UploadToTableRequest body = new UploadToTableRequest();
		body.setCsvTableDescriptor(new CsvTableDescriptor());
		body.getCsvTableDescriptor().setSeparator("-");
		body.getCsvTableDescriptor().setEscapeCharacter("?");
		body.getCsvTableDescriptor().setQuoteCharacter(":");
		body.setLinesToSkip(12L);
		StringReader reader = new StringReader("1,2,3");
		CsvNullReader csvReader = CSVUtils.createCSVReader(reader, body.getCsvTableDescriptor(), body.getLinesToSkip());
		assertNotNull(csvReader);
		assertEquals('-', csvReader.getSeparator());
		assertEquals('?', csvReader.getEscape());
		assertEquals(':', csvReader.getQuotechar());
		assertEquals(12, csvReader.getSkipLines());
	}
}
