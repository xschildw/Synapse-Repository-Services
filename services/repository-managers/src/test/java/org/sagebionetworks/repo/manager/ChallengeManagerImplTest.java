package org.sagebionetworks.repo.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.Challenge;
import org.sagebionetworks.repo.model.ChallengeDAO;
import org.sagebionetworks.repo.model.ChallengePagedResults;
import org.sagebionetworks.repo.model.ChallengeTeam;
import org.sagebionetworks.repo.model.ChallengeTeamDAO;
import org.sagebionetworks.repo.model.ChallengeTeamPagedResults;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.PaginatedIds;
import org.sagebionetworks.repo.model.SubmissionTeam;
import org.sagebionetworks.repo.model.SubmissionTeamPagedResults;
import org.sagebionetworks.repo.model.TeamDAO;
import org.sagebionetworks.repo.model.TeamMember;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.web.NotFoundException;

public class ChallengeManagerImplTest {
	
	private ChallengeManagerImpl challengeManager;
	private AuthorizationManager mockAuthorizationManager;
	private ChallengeDAO mockChallengeDAO;
	private ChallengeTeamDAO mockChallengeTeamDAO;
	private TeamDAO mockTeamDAO;
	
	private static final String PROJECT_ID="syn123456";
	private static final String PARTICIPANT_TEAM_ID="987654321";
	private static final String CHALLENGE_ID="99999";
	private static final String CHALLENGE_TEAM_ID="66666";
	
	private static final UserInfo USER_INFO = new UserInfo(false);
	private static final long USER_PRINCIPAL_ID = 1L;
	private static final UserInfo ADMIN_USER = new UserInfo(true);
	private static final long ADMIN_PRINCIPAL_ID = 2L;

	private static Challenge newChallenge() {
		Challenge challenge = new Challenge();
		challenge.setId(CHALLENGE_ID);
		challenge.setParticipantTeamId(PARTICIPANT_TEAM_ID);
		challenge.setProjectId(PROJECT_ID);
		return challenge;
	}
	
	@Before
	public void setUp() throws Exception {
		USER_INFO.setId(USER_PRINCIPAL_ID);
		ADMIN_USER.setId(ADMIN_PRINCIPAL_ID);
		mockChallengeDAO = Mockito.mock(ChallengeDAO.class);
		mockChallengeTeamDAO = Mockito.mock(ChallengeTeamDAO.class);
		mockAuthorizationManager = Mockito.mock(AuthorizationManager.class);
		mockTeamDAO = Mockito.mock(TeamDAO.class);
		challengeManager = new ChallengeManagerImpl(
				mockChallengeDAO, mockChallengeTeamDAO, 
				mockAuthorizationManager, mockTeamDAO);
	}

	@Test
	public void testCreate() throws Exception {
		Challenge challenge = newChallenge();
		Challenge created = newChallenge();
		created.setId("111");
		when(mockChallengeDAO.create(challenge)).thenReturn(created);
		when(mockAuthorizationManager.canAccess(USER_INFO, PROJECT_ID, 
				ObjectType.ENTITY, ACCESS_TYPE.CREATE)).thenReturn(
						AuthorizationManagerUtil.AUTHORIZED);
		
		assertEquals(created, challengeManager.createChallenge(USER_INFO, challenge));
		verify(mockAuthorizationManager).canAccess(USER_INFO, PROJECT_ID, 
				ObjectType.ENTITY, ACCESS_TYPE.CREATE);
	}
	
	@Test(expected=UnauthorizedException.class)
	public void testCreateUnathorized() throws Exception {
		Challenge challenge = newChallenge();
		when(mockAuthorizationManager.canAccess(USER_INFO, PROJECT_ID, 
				ObjectType.ENTITY, ACCESS_TYPE.CREATE)).thenReturn(
						AuthorizationManagerUtil.ACCESS_DENIED);
		challengeManager.createChallenge(USER_INFO, challenge);
	}
	
	@Test
	public void testGetChallengeByProjectId() throws Exception {
		Challenge created = newChallenge();
		created.setId("111");
		when(mockChallengeDAO.getForProject(PROJECT_ID)).thenReturn(created);
		when(mockAuthorizationManager.canAccess(USER_INFO, PROJECT_ID, 
				ObjectType.ENTITY, ACCESS_TYPE.READ)).thenReturn(
						AuthorizationManagerUtil.AUTHORIZED);
		
		assertEquals(created, challengeManager.getChallengeByProjectId(USER_INFO, PROJECT_ID));
		verify(mockAuthorizationManager).canAccess(USER_INFO, PROJECT_ID, 
				ObjectType.ENTITY, ACCESS_TYPE.READ);
	}

	@Test(expected=UnauthorizedException.class)
	public void getChallengeForProjectIdUnathorized() throws Exception {
		when(mockAuthorizationManager.canAccess(USER_INFO, PROJECT_ID, 
				ObjectType.ENTITY, ACCESS_TYPE.READ)).thenReturn(
						AuthorizationManagerUtil.ACCESS_DENIED);
		challengeManager.getChallengeByProjectId(USER_INFO, PROJECT_ID);
	}
	
	@Test
	public void testListChallengesForParticipant() throws Exception {
		UserInfo requester = new UserInfo(false);
		long limit = 10L;
		long offset = 0L;
		long participantId = 123L;
		Challenge publicChallenge = newChallenge(); publicChallenge.setId("123");
		Challenge privateChallenge = newChallenge(); privateChallenge.setId("456");
		List<Challenge> participantsPublicChallenges = Collections.singletonList(newChallenge());
		List<Challenge> participantsPrivateChallenges = new ArrayList<Challenge>();
		participantsPrivateChallenges.add(publicChallenge);
		participantsPrivateChallenges.add(privateChallenge);
		
		when(mockChallengeDAO.listForUser(participantId, limit, offset)).thenReturn(participantsPrivateChallenges);
		when(mockChallengeDAO.listForUserCount(participantId)).thenReturn((long)participantsPrivateChallenges.size());
		// requester can see only the public challenge
		when(mockChallengeDAO.listForUser(participantId, requester.getGroups(), limit, offset)).thenReturn(participantsPublicChallenges);
		when(mockChallengeDAO.listForUserCount(participantId, requester.getGroups())).thenReturn((long)participantsPublicChallenges.size());
		
		ChallengePagedResults expected = new ChallengePagedResults();
		expected.setResults(participantsPublicChallenges);
		expected.setTotalNumberOfResults((long)participantsPublicChallenges.size());
		assertEquals(expected, challengeManager.listChallengesForParticipant(requester, participantId, limit, offset));

		// now check administrative access
		requester = new UserInfo(true);
		
		expected = new ChallengePagedResults();
		expected.setResults(participantsPrivateChallenges);
		expected.setTotalNumberOfResults((long)participantsPrivateChallenges.size());
		assertEquals(expected, challengeManager.listChallengesForParticipant(requester, participantId, limit, offset));
	}

	@Test
	public void testUpdateChallenge() throws Exception {
		Challenge challenge = newChallenge(); 
		challenge.setId("111");
		Challenge updated = newChallenge(); 
		updated.setId("111"); 
		updated.setParticipantTeamId("999");
		when(mockChallengeDAO.update(challenge)).thenReturn(updated);
		when(mockAuthorizationManager.canAccess(USER_INFO, PROJECT_ID, 
				ObjectType.ENTITY, ACCESS_TYPE.UPDATE)).thenReturn(
						AuthorizationManagerUtil.AUTHORIZED);
		
		assertEquals(updated, challengeManager.updateChallenge(USER_INFO, challenge));
		verify(mockAuthorizationManager).canAccess(USER_INFO, PROJECT_ID, 
				ObjectType.ENTITY, ACCESS_TYPE.UPDATE);
	}
	
	@Test(expected=UnauthorizedException.class)
	public void testUpdateChallengeUnathorized() throws Exception {
		Challenge challenge = newChallenge();
		when(mockAuthorizationManager.canAccess(USER_INFO, PROJECT_ID, 
				ObjectType.ENTITY, ACCESS_TYPE.UPDATE)).thenReturn(
						AuthorizationManagerUtil.ACCESS_DENIED);
		challengeManager.updateChallenge(USER_INFO, challenge);
	}
	
	@Test
	public void testDeleteChallenge() throws Exception {
		Long challengeId = 111L;
		Challenge challenge = newChallenge(); 
		challenge.setId(challengeId.toString());
		when(mockChallengeDAO.get(challengeId)).thenReturn(challenge);
		when(mockAuthorizationManager.canAccess(USER_INFO, PROJECT_ID, 
				ObjectType.ENTITY, ACCESS_TYPE.DELETE)).thenReturn(
						AuthorizationManagerUtil.AUTHORIZED);
		
		challengeManager.deleteChallenge(USER_INFO, challengeId);
		verify(mockChallengeDAO).get(challengeId);
		verify(mockAuthorizationManager).canAccess(USER_INFO, PROJECT_ID, 
				ObjectType.ENTITY, ACCESS_TYPE.DELETE);
		verify(mockChallengeDAO).delete(challengeId);
	}
	
	@Test(expected=UnauthorizedException.class)
	public void testDeleteChallengeUnathorized() throws Exception {
		Long challengeId = 111L;
		Challenge challenge = newChallenge(); 
		challenge.setId(challengeId.toString());
		when(mockChallengeDAO.get(challengeId)).thenReturn(challenge);
		when(mockAuthorizationManager.canAccess(USER_INFO, PROJECT_ID, 
				ObjectType.ENTITY, ACCESS_TYPE.DELETE)).thenReturn(
						AuthorizationManagerUtil.ACCESS_DENIED);
		challengeManager.deleteChallenge(USER_INFO, challengeId);
	}
	
	@Test
	public void testListParticipantsInChallenge() throws Exception {
		Long challengeId = 111L;
		Challenge challenge = newChallenge(); 
		challenge.setId(challengeId.toString());
		Boolean affiliated=null; // return all participants
		long limit = 10L;
		long offset = 0L;
		List<Long> participants = Arrays.asList(new Long[]{111L, 222L, 333L});
		PaginatedIds expected = new PaginatedIds();
		List<String> stringIds = new ArrayList<String>();
		for (Long id : participants) stringIds.add(id.toString());
		expected.setResults(stringIds);
		expected.setTotalNumberOfResults((long)participants.size());
		
		when(mockAuthorizationManager.canAccess(USER_INFO, PROJECT_ID, 
				ObjectType.ENTITY, ACCESS_TYPE.READ)).thenReturn(
						AuthorizationManagerUtil.AUTHORIZED);
		
		when(mockChallengeDAO.get(challengeId)).thenReturn(challenge);
		when(mockChallengeDAO.listParticipants(challengeId, affiliated, limit, offset)).
			thenReturn(participants);
		when(mockChallengeDAO.listParticipantsCount(challengeId, affiliated)).
			thenReturn((long)participants.size());
		
		assertEquals(expected, challengeManager.listParticipantsInChallenge(USER_INFO,
				challengeId, affiliated, limit, offset));
	}

	@Test(expected=UnauthorizedException.class)
	public void testListParticipantsInChallengeUnauthorized() throws Exception {
		Long challengeId = 111L;
		Challenge challenge = newChallenge(); 
		challenge.setId(challengeId.toString());
		Boolean affiliated=null;
		long limit = 10L;
		long offset = 0L;
		
		when(mockAuthorizationManager.canAccess(USER_INFO, PROJECT_ID, 
				ObjectType.ENTITY, ACCESS_TYPE.READ)).thenReturn(
						AuthorizationManagerUtil.ACCESS_DENIED);
		
		when(mockChallengeDAO.get(challengeId)).thenReturn(challenge);
		
		challengeManager.listParticipantsInChallenge(USER_INFO,
				challengeId, affiliated, limit, offset);
	}
	
	private static ChallengeTeam newChallengeTeam(String challengeId, String teamId) {
		ChallengeTeam challengeTeam = new ChallengeTeam();
		challengeTeam.setChallengeId(challengeId);
		challengeTeam.setMessage("Join Our Team!!");
		challengeTeam.setTeamId(teamId);
		return challengeTeam;
	}
	
	@Test
	public void testCanCUDChallengeTeam_Admin() throws Exception {
		// admin is always authorized
		assertTrue(challengeManager.canCreateUpdateOrDeleteChallengeTeam(ADMIN_USER, null).getAuthorized());
	}
	
	@Test
	public void testCanCUDChallengeTeam_InChallAndAdminOnTeam() throws Exception {
		// if you are in the challenge and an admin on the team, then you are authorized
		Challenge challenge = newChallenge();
		when(mockChallengeDAO.get(Long.parseLong(challenge.getId()))).thenReturn(challenge);
		ChallengeTeam challengeTeam = newChallengeTeam(challenge.getId(), CHALLENGE_TEAM_ID);
		TeamMember participantTeamMember = new TeamMember();
		when(mockTeamDAO.getMember(PARTICIPANT_TEAM_ID, USER_INFO.getId().toString())).thenReturn(participantTeamMember);
		TeamMember registeredTeamMember = new TeamMember();
		registeredTeamMember.setIsAdmin(true);
		when(mockTeamDAO.getMember(CHALLENGE_TEAM_ID, USER_INFO.getId().toString())).thenReturn(registeredTeamMember);
		assertTrue(challengeManager.canCreateUpdateOrDeleteChallengeTeam(USER_INFO, challengeTeam).getAuthorized());
	}
	
	@Test
	public void testCanCUDChallengeTeam_NotInChallIsTeamAdmin() throws Exception {
		// if you are in the challenge and an admin on the team, then you are authorized
		Challenge challenge = newChallenge();
		when(mockChallengeDAO.get(Long.parseLong(challenge.getId()))).thenReturn(challenge);
		ChallengeTeam challengeTeam = newChallengeTeam(challenge.getId(), CHALLENGE_TEAM_ID);
		TeamMember registeredTeamMember = new TeamMember();
		registeredTeamMember.setIsAdmin(true);
		when(mockTeamDAO.getMember(CHALLENGE_TEAM_ID, USER_INFO.getId().toString())).thenReturn(registeredTeamMember);

		// if you are not in the challenge then you are not authorized
		when(mockTeamDAO.getMember(PARTICIPANT_TEAM_ID, USER_INFO.getId().toString())).thenThrow(new NotFoundException());
		assertFalse(challengeManager.canCreateUpdateOrDeleteChallengeTeam(USER_INFO, challengeTeam).getAuthorized());
	}
	
	@Test
	public void testCanCUDChallengeTeam_InChallAndNotInTeam() throws Exception {
		// if you are in the challenge and an admin on the team, then you are authorized
		Challenge challenge = newChallenge();
		when(mockChallengeDAO.get(Long.parseLong(challenge.getId()))).thenReturn(challenge);
		ChallengeTeam challengeTeam = newChallengeTeam(challenge.getId(), CHALLENGE_TEAM_ID);

		// if you are not in the team you are trying to register then you are not authorized
		TeamMember participantTeamMember = new TeamMember();
		when(mockTeamDAO.getMember(PARTICIPANT_TEAM_ID, USER_INFO.getId().toString())).thenReturn(participantTeamMember);
		when(mockTeamDAO.getMember(CHALLENGE_TEAM_ID, USER_INFO.getId().toString())).thenThrow(new NotFoundException());
		assertFalse(challengeManager.canCreateUpdateOrDeleteChallengeTeam(USER_INFO, challengeTeam).getAuthorized());
		
	}

	@Test
	public void testCanCUDChallengeTeam_InChallAndNotTeamAdmin() throws Exception {
		// if you are in the challenge and an admin on the team, then you are authorized
		Challenge challenge = newChallenge();
		when(mockChallengeDAO.get(Long.parseLong(challenge.getId()))).thenReturn(challenge);
		ChallengeTeam challengeTeam = newChallengeTeam(challenge.getId(), CHALLENGE_TEAM_ID);
		TeamMember registeredTeamMember = new TeamMember();
		when(mockTeamDAO.getMember(CHALLENGE_TEAM_ID, USER_INFO.getId().toString())).thenReturn(registeredTeamMember);

		// if you are in the team but not an admin, you are not authorized
		registeredTeamMember.setIsAdmin(false);
		when(mockTeamDAO.getMember(CHALLENGE_TEAM_ID, USER_INFO.getId().toString())).thenReturn(registeredTeamMember);
		assertFalse(challengeManager.canCreateUpdateOrDeleteChallengeTeam(USER_INFO, challengeTeam).getAuthorized());
	}
	
	@Test
	public void testCreateChallengeTeam() throws Exception {
		Challenge challenge = newChallenge();
		ChallengeTeam challengeTeam = newChallengeTeam(challenge.getId(), CHALLENGE_TEAM_ID);
		ChallengeTeam created = newChallengeTeam(challenge.getId(), CHALLENGE_TEAM_ID);
		created.setId("222");
		when(mockChallengeTeamDAO.create(challengeTeam)).thenReturn(created);
		// by using an admin user we cause the authorization check to return 'true'
		assertEquals(created, challengeManager.createChallengeTeam(ADMIN_USER, challengeTeam));
	}

	@Test(expected=UnauthorizedException.class)
	public void testCreateChallengeTeamUnAuthorized() throws Exception {
		Challenge challenge = newChallenge();
		ChallengeTeam challengeTeam = newChallengeTeam(challenge.getId(), CHALLENGE_TEAM_ID);
		when(mockChallengeDAO.get(Long.parseLong(challenge.getId()))).thenReturn(challenge);
		when(mockTeamDAO.getMember(PARTICIPANT_TEAM_ID, USER_INFO.getId().toString())).thenThrow(new NotFoundException());
		challengeManager.createChallengeTeam(USER_INFO, challengeTeam);
	}

	@Test
	public void testListChallengeTeams() throws Exception {
		Challenge challenge = newChallenge();
		ChallengeTeam challengeTeam = newChallengeTeam(challenge.getId(), CHALLENGE_TEAM_ID);
		when(mockChallengeDAO.get(Long.parseLong(challenge.getId()))).thenReturn(challenge);
		
		long challengeId = Long.parseLong(challenge.getId());
		List<ChallengeTeam> teams = Collections.singletonList(challengeTeam);
		when(mockChallengeTeamDAO.listForChallenge(challengeId, 10L, 0L)).thenReturn(teams);
		when(mockChallengeTeamDAO.listForChallengeCount(challengeId)).thenReturn((long)teams.size());
		ChallengeTeamPagedResults expected = new ChallengeTeamPagedResults();
		expected.setResults(teams);
		expected.setTotalNumberOfResults((long)teams.size());
		when(mockAuthorizationManager.canAccess(USER_INFO, PROJECT_ID, 
				ObjectType.ENTITY, ACCESS_TYPE.READ)).thenReturn(
						AuthorizationManagerUtil.AUTHORIZED);
		assertEquals(expected,
		challengeManager.listChallengeTeams(USER_INFO, challengeId, 10L, 0L));
	}
	
	@Test(expected=UnauthorizedException.class)
	public void testListChallengeTeamsUnauthorized() throws Exception {
		Challenge challenge = newChallenge();
		when(mockChallengeDAO.get(Long.parseLong(challenge.getId()))).thenReturn(challenge);
		
		long challengeId = Long.parseLong(challenge.getId());
		when(mockAuthorizationManager.canAccess(USER_INFO, PROJECT_ID, 
				ObjectType.ENTITY, ACCESS_TYPE.READ)).thenReturn(
						AuthorizationManagerUtil.ACCESS_DENIED);
		challengeManager.listChallengeTeams(USER_INFO, challengeId, 10L, 0L);
	}
	
	@Test
	public void testListRegistratableTeams() throws Exception {
		Challenge challenge = newChallenge();
		when(mockChallengeDAO.get(Long.parseLong(challenge.getId()))).thenReturn(challenge);
		
		long challengeId = Long.parseLong(challenge.getId());
		List<String> teamIds = Arrays.asList(new String[]{"123", "456"});
		when(mockChallengeTeamDAO.listRegistratable(challengeId, USER_PRINCIPAL_ID, 10L, 0L)).thenReturn(teamIds);
		when(mockChallengeTeamDAO.listRegistratableCount(challengeId, USER_PRINCIPAL_ID)).thenReturn((long)teamIds.size());
		PaginatedIds expected = new PaginatedIds();
		expected.setResults(teamIds);
		expected.setTotalNumberOfResults((long)teamIds.size());
		when(mockAuthorizationManager.canAccess(USER_INFO, PROJECT_ID, 
				ObjectType.ENTITY, ACCESS_TYPE.READ)).thenReturn(
						AuthorizationManagerUtil.AUTHORIZED);
		assertEquals(expected,
		challengeManager.listRegistratableTeams(USER_INFO, challengeId, 10L, 0L));
	}
	
	@Test(expected=UnauthorizedException.class)
	public void testListRegistratableTeamsUnauthorized() throws Exception {
		Challenge challenge = newChallenge();
		when(mockChallengeDAO.get(Long.parseLong(challenge.getId()))).thenReturn(challenge);
		
		long challengeId = Long.parseLong(challenge.getId());
		when(mockAuthorizationManager.canAccess(USER_INFO, PROJECT_ID, 
				ObjectType.ENTITY, ACCESS_TYPE.READ)).thenReturn(
						AuthorizationManagerUtil.ACCESS_DENIED);
		challengeManager.listRegistratableTeams(USER_INFO, challengeId, 10L, 0L);
	}

	@Test
	public void testUpdateChallengeTeam() throws Exception {
		Challenge challenge = newChallenge();
		ChallengeTeam challengeTeam = newChallengeTeam(challenge.getId(), CHALLENGE_TEAM_ID);
		ChallengeTeam updated = newChallengeTeam(challenge.getId(), CHALLENGE_TEAM_ID);
		updated.setId("222");
		when(mockChallengeTeamDAO.update(challengeTeam)).thenReturn(updated);
		// by using an admin user we cause the authorization check to return 'true'
		assertEquals(updated, challengeManager.updateChallengeTeam(ADMIN_USER, challengeTeam));
	}

	@Test(expected=UnauthorizedException.class)
	public void testUpdateChallengeTeamUnAuthorized() throws Exception {
		Challenge challenge = newChallenge();
		ChallengeTeam challengeTeam = newChallengeTeam(challenge.getId(), CHALLENGE_TEAM_ID);
		when(mockChallengeDAO.get(Long.parseLong(challenge.getId()))).thenReturn(challenge);
		when(mockTeamDAO.getMember(PARTICIPANT_TEAM_ID, USER_INFO.getId().toString())).thenThrow(new NotFoundException());
		challengeManager.updateChallengeTeam(USER_INFO, challengeTeam);
	}

	@Test
	public void testDeleteChallengeTeam() throws Exception {
		Challenge challenge = newChallenge();
		ChallengeTeam challengeTeam = newChallengeTeam(challenge.getId(), CHALLENGE_TEAM_ID);
		challengeTeam.setId("222");
		// by using an admin user we cause the authorization check to return 'true'
		challengeManager.deleteChallengeTeam(ADMIN_USER, Long.parseLong(challengeTeam.getId()));
		verify(mockChallengeTeamDAO).delete(Long.parseLong(challengeTeam.getId()));
	}

	@Test(expected=UnauthorizedException.class)
	public void testDeleteChallengeTeamUnAuthorized() throws Exception {
		Challenge challenge = newChallenge();
		ChallengeTeam challengeTeam = newChallengeTeam(challenge.getId(), CHALLENGE_TEAM_ID);
		Long challengeTeamId = 222L;
		challengeTeam.setId(challengeTeamId.toString());
		when(mockChallengeDAO.get(Long.parseLong(challenge.getId()))).thenReturn(challenge);
		when(mockChallengeTeamDAO.get(challengeTeamId)).thenReturn(challengeTeam);
		when(mockTeamDAO.getMember(PARTICIPANT_TEAM_ID, USER_INFO.getId().toString())).thenThrow(new NotFoundException());
		challengeManager.deleteChallengeTeam(USER_INFO, challengeTeamId);
	}

	@Test
	public void testListSubmissionTeams() throws Exception {
		Long challengeId = 111L;
		SubmissionTeam submissionTeam = new SubmissionTeam();
		submissionTeam.setChallengeId(challengeId.toString());
		submissionTeam.setIsRegistered(false);
		submissionTeam.setTeamId(CHALLENGE_TEAM_ID);
		List<SubmissionTeam> submissionTeams = Collections.singletonList(submissionTeam);
		when(mockChallengeTeamDAO.listSubmissionTeams(challengeId, USER_PRINCIPAL_ID, 10L, 0L)).thenReturn(submissionTeams);
		when(mockChallengeTeamDAO.listSubmissionTeamsCount(challengeId, USER_PRINCIPAL_ID)).thenReturn((long)submissionTeams.size());
		SubmissionTeamPagedResults expected = new SubmissionTeamPagedResults();
		expected.setResults(submissionTeams);
		expected.setTotalNumberOfResults((long)submissionTeams.size());
		assertEquals(expected,
		challengeManager.listSubmissionTeams(USER_INFO, challengeId, USER_PRINCIPAL_ID, 10L, 0L));
	}
	
	@Test(expected=UnauthorizedException.class)
	public void testListSubmissionTeamsUnauthorized() throws Exception {
		Long challengeId = 111L;
		challengeManager.listSubmissionTeams(USER_INFO, challengeId, ADMIN_PRINCIPAL_ID, 10L, 0L);
	}
	
	
}
