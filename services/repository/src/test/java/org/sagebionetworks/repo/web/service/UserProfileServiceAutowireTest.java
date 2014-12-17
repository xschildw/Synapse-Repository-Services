package org.sagebionetworks.repo.web.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserGroupHeader;
import org.sagebionetworks.repo.model.UserGroupHeaderResponsePage;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.principal.PrincipalAliasDAO;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.controller.AbstractAutowiredControllerTestBase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 *
 */
public class UserProfileServiceAutowireTest extends AbstractAutowiredControllerTestBase {
	
	@Autowired
	UserManager userManger;
	@Autowired
	UserProfileService userProfileService;

	@Autowired
	private PrincipalAliasDAO principalAliasDAO;
	
	private List<Long> principalsToDelete;

	Long principalOne;
	Long principalTwo;
	Long principalThree;
	UserInfo admin;

	@Before
	public void before() throws NotFoundException{
		principalsToDelete = new LinkedList<Long>();
		// Get the admin info
		admin = userManger.getUserInfo(AuthorizationConstants.BOOTSTRAP_PRINCIPAL.TEST_ADMIN_USER.getPrincipalId());
		// Create two users
		
		// Create a user Profile
		NewUser nu = new NewUser();
		nu.setFirstName("James");
		nu.setLastName("Bond");
		nu.setUserName("007");
		nu.setEmail("superSpy@Spies.org");
		principalOne = userManger.createUser(nu);
		principalsToDelete.add(principalOne);
		
		// Create another profile
		nu = new NewUser();
		nu.setFirstName("Jack");
		nu.setLastName("Black");
		nu.setUserName("random");
		nu.setEmail("super@duper.org");
		principalTwo = userManger.createUser(nu);
		principalsToDelete.add(principalTwo);
		
		// Create another profile
		nu = new NewUser();
		nu.setFirstName("Cate");
		nu.setLastName("Archer");
		nu.setUserName("cate001");
		nu.setEmail("cate@Spies.org");
		principalThree = userManger.createUser(nu);
		principalsToDelete.add(principalThree);
		
		// refresh the cache here
		userProfileService.refreshCache();
	}
	
	@After
	public void after(){
		if(principalsToDelete != null){
			for(Long id: principalsToDelete){
				try {
					userManger.deletePrincipal(admin, id);
				} catch (Exception e) {}
			}
		}
	}
	
	@Test
	public void testPrivateEmailAndOpenId() throws NumberFormatException, DatastoreException, UnauthorizedException, NotFoundException{
		// Each user can see the others profile
		UserProfile profile =userProfileService.getUserProfileByOwnerId(principalOne, principalTwo.toString());
		assertNotNull(profile);
		assertEquals(principalTwo.toString(), profile.getOwnerId());
		assertEquals("This is deprecated and should always be null",null, profile.getEmail());
		assertEquals("One user should not be able to see the Emails of another user.",null, profile.getEmails());
		assertEquals("One user should not be able to see the OpenIds of another user.",null, profile.getOpenIds());
		assertEquals("One user should be able to see the username of another user.","random", profile.getUserName());
		// We should be able see our own data
		profile =userProfileService.getUserProfileByOwnerId(principalTwo, principalTwo.toString());
		assertNotNull(profile);
		assertEquals(principalTwo.toString(), profile.getOwnerId());
		List<String> expected = new LinkedList<String>();
		expected.add("super@duper.org");
		assertEquals("A user must be able to see their own email's", expected, profile.getEmails());
	}
	
	@Test
	public void testHeaders() throws DatastoreException, NotFoundException{
		UserGroupHeaderResponsePage ughrp = userProfileService.getUserGroupHeadersByPrefix("j", 0, Integer.MAX_VALUE, null, null);
		assertNotNull(ughrp);
		assertNotNull(ughrp.getChildren());
		assertTrue(ughrp.getChildren().size() >= 2);
		// Get the ID of all results
		Set<Long> resultSet = new HashSet<Long>();
		for(UserGroupHeader ugh: ughrp.getChildren()){
			Long ownerId = Long.parseLong(ugh.getOwnerId());
			resultSet.add(ownerId);
			assertEquals("Email should always be null", null, ugh.getEmail());
			assertEquals("Email should always be null", null, ugh.getEmail());
			if(principalOne.equals(ownerId)){
				assertEquals("James", ugh.getFirstName());
				assertEquals("Bond", ugh.getLastName());
				assertEquals("007", ugh.getUserName());
			}
		}
		
		assertTrue("Failed to find the user with a 'j' prefix query",resultSet.contains(principalOne));
		assertTrue("Failed to find the user with a 'j' prefix query",resultSet.contains(principalTwo));
	}

	@Test
	public void testGetUserGroupHeadersNoFilter() throws ServletException, IOException, DatastoreException, NotFoundException {
		UserGroupHeaderResponsePage ughrp = userProfileService.getUserGroupHeadersByPrefix("", 0, Integer.MAX_VALUE, null, null);
		assertNotNull(ughrp);
		List<UserGroupHeader> children = ughrp.getChildren();
		assertNotNull(children);
		
		assertTrue(ughrp.getChildren().size() >= 2);

		Set<Long> resultSet = new HashSet<Long>();	
		for (UserGroupHeader ugh : children) {
			Long ownerId = Long.parseLong(ugh.getOwnerId());
			resultSet.add(ownerId);
		}
		// spot check: should find first 15 alphabetical names
		assertTrue("Failed to find the user with a '' prefix query",resultSet.contains(principalOne));
		assertTrue("Failed to find the user with a '' prefix query",resultSet.contains(principalTwo));
		assertTrue("Failed to find the user with a '' prefix query",resultSet.contains(principalThree));
	}
	

	@Test
	public void testGetUserGroupHeadersWithFilterUsername() throws ServletException, IOException, DatastoreException, NotFoundException {
		UserGroupHeaderResponsePage ughrp = userProfileService.getUserGroupHeadersByPrefix("cate001", 0, Integer.MAX_VALUE, null, null);
		assertNotNull(ughrp);
		assertNotNull(ughrp.getChildren());		
		assertTrue(ughrp.getChildren().size() == 1);
		assertEquals(principalThree.toString(), ughrp.getChildren().get(0).getOwnerId());
	}

	
	@Test
	public void testGetUserGroupHeadersWithFilterByLastName() throws ServletException, IOException, DatastoreException, NotFoundException {
		UserGroupHeaderResponsePage ughrp = userProfileService.getUserGroupHeadersByPrefix("B", 0, Integer.MAX_VALUE, null, null);
		assertNotNull(ughrp);
		List<UserGroupHeader> children = ughrp.getChildren();
		assertNotNull(children);
		
		assertTrue(ughrp.getChildren().size() >= 2);
		// Get the ID of all results
		Set<Long> resultSet = new HashSet<Long>();
		for(UserGroupHeader ugh: ughrp.getChildren()){
			Long ownerId = Long.parseLong(ugh.getOwnerId());
			resultSet.add(ownerId);
		}
		
		assertTrue("Failed to find the user with a 'B' prefix query",resultSet.contains(principalOne));
		assertTrue("Failed to find the user with a 'B' prefix query",resultSet.contains(principalTwo));
	}
}
