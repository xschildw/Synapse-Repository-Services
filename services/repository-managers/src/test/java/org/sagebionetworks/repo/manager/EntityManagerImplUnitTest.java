package org.sagebionetworks.repo.manager;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import java.sql.Date;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.repo.manager.backup.NodeBackupManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.NamedAnnotations;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeBackup;
import org.sagebionetworks.repo.model.NodeRevisionBackup;
import org.sagebionetworks.repo.model.QueryResults;
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
		
		entityManager.changeEntityType(mockUser, entityId, targetTypeName, "");
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testChangeEntityInvalid2() throws Exception {
		String userId = "userA";
		String entityId = "syn01";
		String targetTypeName = "invalidtype";
		
		when(mockUserManager.getUserInfo(userId)).thenReturn(mockUser);
		when(mockPermissionsManager.hasAccess(entityId, ACCESS_TYPE.READ, mockUser)).thenReturn(true);
		when(mockPermissionsManager.hasAccess(entityId, ACCESS_TYPE.UPDATE, mockUser)).thenReturn(true);
		
		entityManager.changeEntityType(mockUser, entityId, targetTypeName, "");
	}
	
	@Test
	public void testChangeEntityType() throws Exception {
		Node srcNode = new Node();
		String srcNodeId = "syn1000000";
		srcNode.setDescription("Node description");
		srcNode.setETag("1");
		srcNode.setId(srcNodeId);
		srcNode.setName("nodeName");
		srcNode.setNodeType("phenotypedata");
		srcNode.setVersionComment("This is version 1");
		srcNode.setVersionLabel("1.0.0");
		srcNode.setVersionNumber(new Long(2));
		
		Node expectedNode = new Node();
		expectedNode.setDescription("Node description");
		expectedNode.setETag("1");
		expectedNode.setId(srcNodeId);
		expectedNode.setName("nodeName");
		expectedNode.setNodeType("layer");
		expectedNode.setVersionComment("The type of this entity has changed. All properties that could not be migrated are now annotations.");
		srcNode.setVersionLabel("1.0.0");
		expectedNode.setVersionNumber(new Long(2));
		
		NamedAnnotations srcNamedAnnots = new NamedAnnotations();
		Annotations srcPrimaryAnnots = srcNamedAnnots.getPrimaryAnnotations();
		Annotations srcAdditionalAnnots = srcNamedAnnots.getAdditionalAnnotations();
		// Should stay in primary
		srcPrimaryAnnots.addAnnotation("numSamples", 10L);
		srcPrimaryAnnots.addAnnotation("disease", "cancer");
		// Should stay in additional
		srcAdditionalAnnots.addAnnotation("someDoubleAnnotation", new Double(3.1415));
		// Should move to additional
		srcPrimaryAnnots.addAnnotation("somePrimaryDateAnnot", new Date(1,1,2000));
		
		NamedAnnotations expectedNamedAnnots = new NamedAnnotations();
		Annotations expectedPrimaryAnnots = expectedNamedAnnots.getPrimaryAnnotations();
		Annotations expectedAdditionalAnnots = expectedNamedAnnots.getAdditionalAnnotations();		
		// Should stay in primary
		expectedPrimaryAnnots.addAnnotation("numSamples", srcPrimaryAnnots.getSingleValue("numSamples"));
		expectedPrimaryAnnots.addAnnotation("disease", srcPrimaryAnnots.getSingleValue("disease"));
		// Should stay in additional
		expectedAdditionalAnnots.addAnnotation("someDoubleAnnotation", srcAdditionalAnnots.getSingleValue("someDoubleAnnotation"));
		// Should move to additional
		expectedAdditionalAnnots.addAnnotation("somePrimaryDateAnnot", srcPrimaryAnnots.getSingleValue("somePrimaryDateAnnot"));
		
		when(mockPermissionsManager.hasAccess(srcNodeId, ACCESS_TYPE.READ, mockUser)).thenReturn(true);
		when(mockPermissionsManager.hasAccess(srcNodeId, ACCESS_TYPE.UPDATE, mockUser)).thenReturn(true);
		when(mockNodeManager.get(mockUser, srcNodeId)).thenReturn(srcNode);
		when(mockNodeManager.doesNodeHaveChildren(srcNodeId)).thenReturn(false);
		List<Long> expectedVersionNums = new ArrayList<Long>();
		expectedVersionNums.add(new Long(1));
		expectedVersionNums.add(new Long(2));
		when(mockNodeManager.getAllVersionNumbersForNode(mockUser, srcNodeId)).thenReturn(expectedVersionNums);
		when(mockNodeManager.getAnnotations(mockUser, srcNodeId)).thenReturn(srcNamedAnnots);
		QueryResults<EntityHeader> rsNodeRefs = new QueryResults<EntityHeader>();
		rsNodeRefs.setTotalNumberOfResults(1);
		List<EntityHeader> ehList = new ArrayList<EntityHeader>();
		EntityHeader eh = new EntityHeader();
		eh.setId("syn2000000");
		eh.setName("someEntity");
		eh.setType("someType");
		ehList.add(eh);
		rsNodeRefs.setResults(ehList);
		when(mockNodeManager.getEntityReferences(mockUser, srcNodeId, 2, 0, 100)).thenReturn(rsNodeRefs);
		Set<Node> referrerChildren = new HashSet<Node>();
		Node child = new Node();
		child.setNodeType("link");
		child.setId("syn3000000");
		referrerChildren.add(child);
		when(mockNodeManager.getChildren(mockUser, "syn2000000")).thenReturn(referrerChildren);
		NamedAnnotations srcReferrerLinkChildAnnots = new NamedAnnotations();
		srcReferrerLinkChildAnnots.getPrimaryAnnotations().addAnnotation("targetId", srcNodeId);
		srcReferrerLinkChildAnnots.getPrimaryAnnotations().addAnnotation("linksToClassName", "org.sagebionetworks.repo.model.PhenotypeData");
		when(mockNodeManager.getAnnotations(mockUser, "syn3000000")).thenReturn(srcReferrerLinkChildAnnots);
		NamedAnnotations updReferrerLinkChildAnnots = new NamedAnnotations();
		updReferrerLinkChildAnnots.getPrimaryAnnotations().addAnnotation("targetId", srcNodeId);
		updReferrerLinkChildAnnots.getPrimaryAnnotations().addAnnotation("linksToClassName", "org.sagebionetworks.repo.model.Data");

		
		entityManager.changeEntityType(mockUser, srcNodeId, "layer", "");
		
		verify(mockNodeManager).deleteVersion(mockUser, srcNodeId, new Long(1));
		verify(mockNodeManager).updateAnnotations(mockUser, "syn3000000", updReferrerLinkChildAnnots.getPrimaryAnnotations(), "primary");
		verify(mockNodeManager).update(mockUser, expectedNode, expectedNamedAnnots, false);
	}
	
}
