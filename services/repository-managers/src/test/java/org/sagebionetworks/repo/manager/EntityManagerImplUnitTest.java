package org.sagebionetworks.repo.manager;

import static org.junit.Assert.*;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.Ignore;
import org.mockito.Mockito;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.GenomicData;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.NamedAnnotations;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.User;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.attachment.PresignedUrl;
import org.sagebionetworks.repo.model.attachment.S3AttachmentToken;
import org.sagebionetworks.repo.util.LocationHelper;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.http.HttpMethod;

import com.amazonaws.services.securitytoken.model.Credentials;

public class EntityManagerImplUnitTest {

	private UserManager mockUserManager;
	private PermissionsManager mockPermissionsManager;
	private UserInfo mockUser;
	private EntityManagerImpl entityManager;
	private NodeManager mockNodeManager;
	private S3TokenManager mockS3TokenManager;
	private IdGenerator mocIdGenerator;
	private LocationHelper mocKLocationHelper;
	String userId = "007";
	
	@Before
	public void before(){
		// Create the mocks
		mockPermissionsManager = Mockito.mock(PermissionsManager.class);
		mockUserManager = Mockito.mock(UserManager.class);
		mockNodeManager = Mockito.mock(NodeManager.class);
		mockS3TokenManager = Mockito.mock(S3TokenManager.class);
		mocIdGenerator = Mockito.mock(IdGenerator.class);
		mocKLocationHelper = Mockito.mock(LocationHelper.class);
		mockUser = new UserInfo(false);
		mockUser.setUser(new User());
		mockUser.getUser().setId(userId);
		entityManager = new EntityManagerImpl(mockNodeManager, mockS3TokenManager, mockPermissionsManager, mockUserManager);
	}

	@Test (expected=UnauthorizedException.class)
	public void testValidateReadAccessFail() throws DatastoreException, NotFoundException, UnauthorizedException{
		String userId = "123456";
		String entityId = "abc";
		// return the mock user.
		when(mockUserManager.getUserInfo(userId)).thenReturn(mockUser);
		// Say now to this
		when(mockPermissionsManager.hasAccess(entityId, ACCESS_TYPE.READ, mockUser)).thenReturn(false);
		when(mockPermissionsManager.hasAccess(entityId, ACCESS_TYPE.UPDATE, mockUser)).thenThrow(new IllegalArgumentException("Read and not update should have been checked"));
		entityManager.validateReadAccess(mockUser, entityId);
		
	}
	
	@Test 
	public void testValidateReadAccessPass() throws DatastoreException, NotFoundException, UnauthorizedException{
		String userId = "123456";
		String entityId = "abc";
		// return the mock user.
		when(mockUserManager.getUserInfo(userId)).thenReturn(mockUser);
		// Say now to this
		when(mockPermissionsManager.hasAccess(entityId, ACCESS_TYPE.READ, mockUser)).thenReturn(true);
		when(mockPermissionsManager.hasAccess(entityId, ACCESS_TYPE.UPDATE, mockUser)).thenThrow(new IllegalArgumentException("Read and not update should have been checked"));
		entityManager.validateReadAccess(mockUser, entityId);
	}
	

	@Test (expected=UnauthorizedException.class)
	public void testGetAttachmentUrlNoReadAccess() throws Exception{
		Long tokenId = new Long(456);
		String entityId = "132";
		String userId = "007";
		String expectedPath = S3TokenManagerImpl.createAttachmentPathSlash(entityId, tokenId.toString());
		String expectePreSigneUrl = "I am a presigned url! whooot!";
		when(mockUserManager.getUserInfo(userId)).thenReturn(mockUser);
		// Simulate a 
		when(mockPermissionsManager.hasAccess(entityId, ACCESS_TYPE.READ, mockUser)).thenReturn(false);
		when(mocKLocationHelper.presignS3GETUrlShortLived(userId, expectedPath)).thenReturn(expectePreSigneUrl);
		// Make the actual call
		PresignedUrl url = entityManager.getAttachmentUrl(userId, entityId, tokenId.toString());
		assertNotNull(url);
		assertEquals(expectePreSigneUrl, url.getPresignedUrl());
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testCreateS3AttachmentTokenNoUpdateAccess() throws NumberFormatException, DatastoreException, NotFoundException, UnauthorizedException, InvalidModelException{
		S3AttachmentToken startToken = new S3AttachmentToken();
		startToken.setFileName(null);
		String almostMd5 = "79054025255fb1a26e4bc422aef54eb4";
		startToken.setMd5(almostMd5);
		Long tokenId = new Long(456);
		String entityId = "132";
		String userId = "007";
		String expectedPath = entityId+"/"+tokenId.toString();
		String expectePreSigneUrl = "I am a presigned url! whooot!";
		when(mocIdGenerator.generateNewId()).thenReturn(tokenId);
		Credentials mockCreds = Mockito.mock(Credentials.class);
		when(mockUserManager.getUserInfo(userId)).thenReturn(mockUser);
		when(mockPermissionsManager.hasAccess(entityId, ACCESS_TYPE.UPDATE, mockUser)).thenReturn(false);
		when(mocKLocationHelper.createFederationTokenForS3(userId,HttpMethod.PUT,expectedPath)).thenReturn(mockCreds);
		when(mocKLocationHelper.presignS3PUTUrl(mockCreds, expectedPath, almostMd5, "image/jpeg")).thenReturn(expectePreSigneUrl);
		// Make the actual call
		S3AttachmentToken endToken = entityManager.createS3AttachmentToken(userId, entityId, startToken);
		assertNotNull(endToken);
		assertEquals(expectePreSigneUrl, endToken.getPresignedUrl());
	}

	@Test (expected=UnauthorizedException.class)
	public void testChangeEntityInvalid1() throws Exception {
		String userId = "userA";
		String entityId = "syn01";
		String targetTypeName = "genomicdata";
		
		when(mockUserManager.getUserInfo(userId)).thenReturn(mockUser);
		when(mockPermissionsManager.hasAccess(entityId, ACCESS_TYPE.READ, mockUser)).thenReturn(false);
		when(mockPermissionsManager.hasAccess(entityId, ACCESS_TYPE.UPDATE, mockUser)).thenReturn(false);
		
		entityManager.changeEntityType(mockUser, entityId, targetTypeName);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testChangeEntityInvalid2() throws Exception {
		String userId = "userA";
		String entityId = "syn01";
		String targetTypeName = "invalidtype";
		
		when(mockUserManager.getUserInfo(userId)).thenReturn(mockUser);
		when(mockPermissionsManager.hasAccess(entityId, ACCESS_TYPE.READ, mockUser)).thenReturn(true);
		when(mockPermissionsManager.hasAccess(entityId, ACCESS_TYPE.UPDATE, mockUser)).thenReturn(true);
		
		entityManager.changeEntityType(mockUser, entityId, targetTypeName);
	}

	@Test
	public void testIsValidTypeChange() throws Exception {
		// TODO: Expand to cover all combos?
		boolean v;
		String s;
		// Should not be able to go to project
		// Note: Might work but not for the right reason...
		s = "project";
		v = EntityManagerImpl.isValidTypeChange("folder", s);
		assertFalse(v);
		// Should be able to go from dataset/study to folder
		s = "folder";
		v = EntityManagerImpl.isValidTypeChange("dataset", s);
		assertFalse(v);
		// Should be able to go from phenotypedata to data
		s = "layer";
		v = EntityManagerImpl.isValidTypeChange("phenotypedata", s);
		assertTrue(v);
		// Should be able to go from data to genomicdata
		s = "genomicdata";
		v = EntityManagerImpl.isValidTypeChange("layer", s);
		assertTrue(v);
		// Should not be able to go from Locationable to non-Locationable
		s = "folder";
		v = EntityManagerImpl.isValidTypeChange("layer", s);
		assertFalse(v);
	}
	
	@Test
	public void testChangeEntityType() throws Exception {
		String userId = "userA";
		String entityId = "syn01";
		String srcType = "data";
		String targetType = "genomicdata";
		
		Node expectedBeforeNode = new Node();
		expectedBeforeNode.setId(entityId);
		expectedBeforeNode.setName("node");
		expectedBeforeNode.setNodeType(srcType);
		
		Node expectedAfterNode = new Node();
		expectedAfterNode.setId(entityId);
		expectedAfterNode.setName("node");
		expectedAfterNode.setNodeType(targetType);
				
		NamedAnnotations expectedBeforeNamedAnnots = new NamedAnnotations();
		Annotations expectedBeforePrimaryAnnots = expectedBeforeNamedAnnots.getPrimaryAnnotations();
		Annotations expectedBeforeAdditionalAnnots = expectedBeforeNamedAnnots.getAdditionalAnnotations();
		// All these should become additional
		// TODO: Add cases where names overlap
		expectedBeforePrimaryAnnots.addAnnotation("datePrimaryKey1", new Date("01/01/2000"));
		expectedBeforePrimaryAnnots.addAnnotation("datePrimaryKey2", new Date("01/02/2000"));
		expectedBeforePrimaryAnnots.addAnnotation("doublePrimaryKey1", 1.0);
		expectedBeforePrimaryAnnots.addAnnotation("longPrimaryKey1", 1L);
		expectedBeforePrimaryAnnots.addAnnotation("stringPrimaryKey1", "string1");
		expectedBeforePrimaryAnnots.addAnnotation("stringPrimaryKey2", "string2");
		// Should stay primary
		expectedBeforePrimaryAnnots.addAnnotation("name", "someName");
		expectedBeforePrimaryAnnots.addAnnotation("description", "someDescription");
		// Should stay additional
		expectedBeforeAdditionalAnnots.addAnnotation("dateAdditionalKey1", new Date("01/03/2000"));
		expectedBeforeAdditionalAnnots.addAnnotation("doubleAdditionalKey1", 2.0);
		expectedBeforeAdditionalAnnots.addAnnotation("longAdditionalKey1", 2L);
		expectedBeforeAdditionalAnnots.addAnnotation("stringAdditionalKey1", "string2");
		
		NamedAnnotations expectedAfterAnnots = new NamedAnnotations();
		Annotations expectedAfterPrimaryAnnots = expectedAfterAnnots.getPrimaryAnnotations();
		Annotations expectedAfterAdditionalAnnots = expectedAfterAnnots.getAdditionalAnnotations();
		// All primary annotations have become additional before calling NodeManager.update()
		expectedAfterAdditionalAnnots.addAnnotation("dateAdditionalKey1", new Date("01/03/2000"));
		expectedAfterAdditionalAnnots.addAnnotation("doubleAdditionalKey1", 2.0);
		expectedAfterAdditionalAnnots.addAnnotation("longAdditionalKey1", 2L);
		expectedAfterAdditionalAnnots.addAnnotation("stringAdditionalKey1", "string2");
		expectedAfterAdditionalAnnots.addAnnotation("datePrimaryKey1", new Date("01/01/2000"));
		expectedAfterAdditionalAnnots.addAnnotation("datePrimaryKey2", new Date("01/02/2000"));
		expectedAfterAdditionalAnnots.addAnnotation("doublePrimaryKey1", 1.0);
		expectedAfterAdditionalAnnots.addAnnotation("longPrimaryKey1", 1L);
		expectedAfterAdditionalAnnots.addAnnotation("stringPrimaryKey1", "string1");
		expectedAfterAdditionalAnnots.addAnnotation("stringPrimaryKey2", "string2");
		// Except these that should stay primary
		expectedAfterPrimaryAnnots.addAnnotation("name", "someName");
		expectedAfterPrimaryAnnots.addAnnotation("description", "someDescription");
		
		when(mockUserManager.getUserInfo(userId)).thenReturn(mockUser);
		when(mockPermissionsManager.hasAccess(entityId, ACCESS_TYPE.READ, mockUser)).thenReturn(true);
		when(mockPermissionsManager.hasAccess(entityId, ACCESS_TYPE.UPDATE, mockUser)).thenReturn(true);
		when(mockNodeManager.get(mockUser, entityId)).thenReturn(expectedBeforeNode);
		when(mockNodeManager.getAnnotations(mockUser, entityId)).thenReturn(expectedBeforeNamedAnnots);
		List<Long> revs = Arrays.asList(0L);
		when(mockNodeManager.getAllVersionNumbersForNode(mockUser, entityId)).thenReturn(revs);
		when(mockNodeManager.getNodeForVersionNumber(mockUser, entityId, revs.get(0))).thenReturn(expectedBeforeNode);
		when(mockNodeManager.getAnnotationsForVersion(mockUser, entityId, revs.get(0))).thenReturn(expectedBeforeNamedAnnots);
		
		entityManager.changeEntityType(mockUser, entityId, targetType);
		verify(mockNodeManager).updateVersion(mockUser, expectedBeforeNode, expectedBeforeNamedAnnots, revs.get(0));
		
	}
}
