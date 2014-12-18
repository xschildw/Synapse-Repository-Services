package org.sagebionetworks.repo.web.migration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.bridge.model.BridgeParticipantDAO;
import org.sagebionetworks.bridge.model.BridgeUserParticipantMappingDAO;
import org.sagebionetworks.bridge.model.Community;
import org.sagebionetworks.bridge.model.CommunityTeamDAO;
import org.sagebionetworks.bridge.model.ParticipantDataDAO;
import org.sagebionetworks.bridge.model.ParticipantDataDescriptorDAO;
import org.sagebionetworks.bridge.model.ParticipantDataId;
import org.sagebionetworks.bridge.model.ParticipantDataStatusDAO;
import org.sagebionetworks.bridge.model.data.ParticipantDataColumnDescriptor;
import org.sagebionetworks.bridge.model.data.ParticipantDataColumnType;
import org.sagebionetworks.bridge.model.data.ParticipantDataDescriptor;
import org.sagebionetworks.bridge.model.data.ParticipantDataRepeatType;
import org.sagebionetworks.bridge.model.data.ParticipantDataRow;
import org.sagebionetworks.bridge.model.data.ParticipantDataStatus;
import org.sagebionetworks.bridge.model.data.value.ParticipantDataStringValue;
import org.sagebionetworks.bridge.model.data.value.ParticipantDataValue;
import org.sagebionetworks.evaluation.model.Evaluation;
import org.sagebionetworks.evaluation.model.EvaluationStatus;
import org.sagebionetworks.evaluation.model.Submission;
import org.sagebionetworks.repo.manager.StorageQuotaManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.UserProfileManager;
import org.sagebionetworks.repo.manager.migration.MigrationManager;
import org.sagebionetworks.repo.manager.team.TeamManager;
import org.sagebionetworks.repo.manager.team.TeamManagerImpl;
import org.sagebionetworks.repo.model.*;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.bootstrap.EntityBootstrapper;
import org.sagebionetworks.repo.model.daemon.BackupRestoreStatus;
import org.sagebionetworks.repo.model.daemon.DaemonStatus;
import org.sagebionetworks.repo.model.daemon.RestoreSubmission;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.dao.table.ColumnModelDAO;
import org.sagebionetworks.repo.model.dao.table.TableRowTruthDAO;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.dao.table.TableModelTestUtils;
import org.sagebionetworks.repo.model.dbo.persistence.DBOSessionToken;
import org.sagebionetworks.repo.model.dbo.persistence.DBOTermsOfUseAgreement;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.PreviewFileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.file.UploadType;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.message.Comment;
import org.sagebionetworks.repo.model.message.MessageToUser;
import org.sagebionetworks.repo.model.migration.ListBucketProvider;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.migration.MigrationTypeCount;
import org.sagebionetworks.repo.model.migration.MigrationTypeCounts;
import org.sagebionetworks.repo.model.migration.MigrationTypeList;
import org.sagebionetworks.repo.model.migration.MigrationUtils;
import org.sagebionetworks.repo.model.migration.RowMetadata;
import org.sagebionetworks.repo.model.migration.RowMetadataResult;
import org.sagebionetworks.repo.model.principal.PrincipalAliasDAO;
import org.sagebionetworks.repo.model.project.S3UploadDestinationSetting;
import org.sagebionetworks.repo.model.project.UploadDestinationListSetting;
import org.sagebionetworks.repo.model.project.UploadDestinationSetting;
import org.sagebionetworks.repo.model.provenance.Activity;
import org.sagebionetworks.repo.model.quiz.PassingRecord;
import org.sagebionetworks.repo.model.quiz.QuizResponse;
import org.sagebionetworks.repo.model.table.ColumnMapper;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.RawRowSet;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.repo.model.v2.dao.V2WikiPageDao;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiPage;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.controller.AbstractAutowiredControllerTestBase;
import org.sagebionetworks.repo.web.service.ServiceProvider;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

/**
 * This is an integration test to test the migration of all tables from start to finish.
 * 
 * The test does the following: 1. the before() method creates at least one object for every type object that must
 * migrate. 2. Create a backup copy of all data. 3. Delete all data in the system. 4. Restore all data from the backup.
 * 
 * NOTE: Whenever a new migration type is added this test must be extended to test that objects migration.
 * 
 * 
 * 
 * @author jmhill
 * 
 */
@DirtiesContext
public class MigrationIntegrationAutowireTest extends AbstractAutowiredControllerTestBase {

	public static final long MAX_WAIT_MS = 30 * 1000; // 10 sec.

	@Autowired
	private DBOBasicDao basicDao;

	@Autowired
	private UserManager userManager;

	@Autowired
	private FileHandleDao fileMetadataDao;

	@Autowired
	private UserProfileManager userProfileManager;

	@Autowired
	private ServiceProvider serviceProvider;

	@Autowired
	private EntityBootstrapper entityBootstrapper;

	@Autowired
	private MigrationManager migrationManager;
	
	@Autowired
	private TeamManager teamManager;

	@Autowired
	private StorageQuotaManager storageQuotaManager;

	@Autowired
	private StorageQuotaAdminDao storageQuotaAdminDao;

	@Autowired
	private UserGroupDAO userGroupDAO;
	
	@Autowired
	private PrincipalAliasDAO principalAliasDAO;

	@Autowired
	private GroupMembersDAO groupMembersDAO;

	@Autowired
	private TeamDAO teamDAO;

	@Autowired
	private CommunityTeamDAO communityTeamDAO;

	@Autowired
	private BridgeParticipantDAO bridgeParticipantDAO;

	@Autowired
	private BridgeUserParticipantMappingDAO bridgeUserParticipantMappingDAO;

	@Autowired
	private ParticipantDataDAO participantDataDAO;

	@Autowired
	private ParticipantDataDescriptorDAO participantDataDescriptorDAO;

	@Autowired
	private ParticipantDataStatusDAO participantDataStatusDAO;

	@Autowired
	private AuthenticationDAO authDAO;

	@Autowired
	private MessageDAO messageDAO;

	@Autowired
	private CommentDAO commentDAO;

	@Autowired
	private MembershipRqstSubmissionDAO membershipRqstSubmissionDAO;

	@Autowired
	private MembershipInvtnSubmissionDAO membershipInvtnSubmissionDAO;

	@Autowired
	private ColumnModelDAO columnModelDao;

	@Autowired
	private TableRowTruthDAO tableRowTruthDao;

	@Autowired
	private V2WikiPageDao v2wikiPageDAO;
	
	@Autowired
	private QuizResponseDAO quizResponseDAO;

	@Autowired
	private ProjectSettingsDAO projectSettingsDAO;

	@Autowired
	private ProjectStatsDAO projectStatsDAO;

	private Long migrationUserId;
	private String migrationUserIdString;
	private UserInfo migrationUserInfo;
	private Long adminUserId;
	private String adminUserIdString;
	private UserInfo adminUserInfo;

	// Activity
	private Activity activity;

	// Entities
	private Project project;
	private FileEntity fileEntity;
	private Community community;
	private Folder folderToTrash;

	// requirement
	private AccessRequirement accessRequirement;

	// approval
	private AccessApproval accessApproval;

	// V2 Wiki page
	private V2WikiPage v2RootWiki;
	private V2WikiPage v2SubWiki;

	// File Handles
	private S3FileHandle handleOne;
	private S3FileHandle markdownOne;
	private PreviewFileHandle preview;

	// Evaluation
	private Evaluation evaluation;
	private Submission submission;

	private HttpServletRequest mockRequest;

	@Before
	public void before() throws Exception {
		mockRequest = Mockito.mock(HttpServletRequest.class);
		when(mockRequest.getServletPath()).thenReturn("/repo/v1");

		// get user IDs
		migrationUserId = BOOTSTRAP_PRINCIPAL.MIGRATION_USER.getPrincipalId();
		migrationUserIdString = migrationUserId.toString();
		migrationUserInfo = userManager.getUserInfo(migrationUserId);
		adminUserId = BOOTSTRAP_PRINCIPAL.TEST_ADMIN_USER.getPrincipalId();
		adminUserIdString = adminUserId.toString();
		adminUserInfo = userManager.getUserInfo(adminUserId);

		resetDatabase();
		createNewUser();
		String sampleFileHandleId = createFileHandles();
		createActivity();
		createEntities();
		createFavorite();
		createProjectSetting();
		createProjectStat();
		createEvaluation();
		createAccessRequirement();
		createAccessApproval();
		createV2WikiPages();
		createDoi();
		createStorageQuota();
		UserGroup sampleGroup = createUserGroups(1);
		createTeamsRequestsAndInvitations(sampleGroup);
		createCredentials(sampleGroup);
		createSessionToken(sampleGroup);
		createTermsOfUseAgreement(sampleGroup);
		createMessages(sampleGroup, sampleFileHandleId);
		createColumnModel();
		UserGroup sampleGroup2 = createUserGroups(2);
		createCommunity(sampleGroup2);
		createParticipantData(sampleGroup);
		createQuizResponse();
	}
	
	private void createProjectSetting() {
		UploadDestinationListSetting settings = new UploadDestinationListSetting();
		settings.setProjectId(project.getId());
		settings.setSettingsType("upload");
		S3UploadDestinationSetting destination = new S3UploadDestinationSetting();
		destination.setUploadType(UploadType.S3);
		settings.setDestinations(Collections.<UploadDestinationSetting> singletonList(destination));
		projectSettingsDAO.create(settings);
	}

	private void createProjectStat() {
		ProjectStat projectStat = new ProjectStat(KeyFactory.stringToKey(project.getId()), adminUserId, new Date());
		projectStatsDAO.update(projectStat);
	}

	private void createQuizResponse() {
		QuizResponse dto = new QuizResponse();
		PassingRecord passingRecord = new PassingRecord();
		passingRecord.setPassed(true);
		passingRecord.setPassedOn(new Date());
		passingRecord.setQuizId(101L);
		passingRecord.setResponseId(222L);
		passingRecord.setScore(7L);
		passingRecord.setUserId(adminUserId.toString());
		adminUserId = BOOTSTRAP_PRINCIPAL.TEST_ADMIN_USER.getPrincipalId();
		dto.setCreatedBy(adminUserId.toString());
		dto.setCreatedOn(new Date());
		dto.setQuizId(101L);
		quizResponseDAO.create(dto, passingRecord);
	}

	private void createColumnModel() throws DatastoreException, NotFoundException, IOException {
		String tableId = "syn123";
		// Create some test column models
		List<ColumnModel> start = TableModelTestUtils.createOneOfEachType();
		// Create each one
		List<ColumnModel> models = new LinkedList<ColumnModel>();
		for (ColumnModel cm : start) {
			models.add(columnModelDao.createColumnModel(cm));
		}
		ColumnMapper mapper = TableModelUtils.createColumnModelColumnMapper(models, false);

		List<String> header = TableModelUtils.getHeaders(models);
		// bind the columns to the entity
		columnModelDao.bindColumnToObject(header, tableId);

		// create some test rows.
		List<Row> rows = TableModelTestUtils.createRows(models, 5);
		RawRowSet set = new RawRowSet(TableModelUtils.getHeaders(models), null, tableId, rows);
		// Append the rows to the table
		tableRowTruthDao.appendRowSetToTable(adminUserIdString, tableId, mapper, set);
		// Append some more rows
		rows = TableModelTestUtils.createRows(models, 6);
		set = new RawRowSet(TableModelUtils.getHeaders(models), null, tableId, rows);
		tableRowTruthDao.appendRowSetToTable(adminUserIdString, tableId, mapper, set);
	}

	public void createNewUser() throws NotFoundException {
		NewUser user = new NewUser();
		user.setUserName(UUID.randomUUID().toString());
		user.setEmail(user.getUserName() + "@test.com");
		Long id = userManager.createUser(user);
		userManager.getUserInfo(id);
	}

	private void resetDatabase() throws Exception {
		// This gives us a chance to also delete the S3 for table rows
		tableRowTruthDao.truncateAllRowData();
		// Before we start this test we want to start with a clean database
		migrationManager.deleteAllData(migrationUserInfo);
		// bootstrap to put back the bootstrap data
		entityBootstrapper.bootstrapAll();
		storageQuotaAdminDao.clear();
		teamManager.bootstrapTeams();
	}

	private void createFavorite() {
		userProfileManager.addFavorite(adminUserInfo, fileEntity.getId());
	}

	private void createDoi() throws Exception {
		serviceProvider.getDoiService().createDoi(adminUserId, project.getId(), ObjectType.ENTITY, 1L);
	}

	private void createActivity() throws Exception {
		activity = new Activity();
		activity.setDescription("some desc");
		activity = serviceProvider.getActivityService().createActivity(adminUserId, activity);
	}

	private void createEvaluation() throws Exception {
		// initialize Evaluations
		evaluation = new Evaluation();
		evaluation.setName("name");
		evaluation.setDescription("description");
		evaluation.setContentSource(project.getId());
		evaluation.setStatus(EvaluationStatus.PLANNED);
		evaluation.setSubmissionInstructionsMessage("instructions");
		evaluation.setSubmissionReceiptMessage("receipt");
		evaluation = serviceProvider.getEvaluationService().createEvaluation(adminUserId, evaluation);
		evaluation = new Evaluation();
		evaluation.setName("name2");
		evaluation.setDescription("description");
		evaluation.setContentSource(project.getId());
		evaluation.setStatus(EvaluationStatus.OPEN);
		evaluation.setSubmissionInstructionsMessage("instructions");
		evaluation.setSubmissionReceiptMessage("receipt");
		evaluation = serviceProvider.getEvaluationService().createEvaluation(adminUserId, evaluation);

		// initialize Participants
		serviceProvider.getEvaluationService().addParticipant(adminUserId, evaluation.getId());

		// initialize Submissions
		submission = new Submission();
		submission.setName("submission1");
		submission.setVersionNumber(1L);
		submission.setEntityId(fileEntity.getId());
		submission.setUserId(adminUserIdString);
		submission.setEvaluationId(evaluation.getId());
		submission = entityServletHelper.createSubmission(submission, adminUserId, fileEntity.getEtag());
	}

	public void createAccessApproval() throws Exception {
		accessApproval = newToUAccessApproval(accessRequirement.getId(), adminUserIdString);
		accessApproval = servletTestHelper.createAccessApproval(dispatchServlet, accessApproval, adminUserId,
				new HashMap<String, String>());
	}

	public void createAccessRequirement() throws Exception {
		// Add an access requirement to this entity
		accessRequirement = newAccessRequirement();
		String entityId = project.getId();
		RestrictableObjectDescriptor entitySubjectId = new RestrictableObjectDescriptor();
		entitySubjectId.setId(entityId);
		entitySubjectId.setType(RestrictableObjectType.ENTITY);
		RestrictableObjectDescriptor evaluationSubjectId = new RestrictableObjectDescriptor();
		assertNotNull(evaluation);
		assertNotNull(evaluation.getId());
		evaluationSubjectId.setId(evaluation.getId());
		evaluationSubjectId.setType(RestrictableObjectType.EVALUATION);

		accessRequirement.setSubjectIds(Arrays.asList(new RestrictableObjectDescriptor[] { entitySubjectId, evaluationSubjectId }));
		accessRequirement = servletTestHelper.createAccessRequirement(dispatchServlet, accessRequirement, adminUserId,
				new HashMap<String, String>());
	}

	private TermsOfUseAccessApproval newToUAccessApproval(Long requirementId, String accessorId) {
		TermsOfUseAccessApproval aa = new TermsOfUseAccessApproval();
		aa.setAccessorId(accessorId);
		aa.setConcreteType(TermsOfUseAccessApproval.class.getName());
		aa.setRequirementId(requirementId);
		return aa;
	}

	public void createV2WikiPages() throws NotFoundException {
		// Using wikiPageDao until wiki service is created

		// Create a V2 Wiki page
		v2RootWiki = new V2WikiPage();
		v2RootWiki.setCreatedBy(adminUserIdString);
		v2RootWiki.setModifiedBy(adminUserIdString);
		v2RootWiki.setAttachmentFileHandleIds(new LinkedList<String>());
		v2RootWiki.getAttachmentFileHandleIds().add(handleOne.getId());
		v2RootWiki.setTitle("Root title");
		v2RootWiki.setMarkdownFileHandleId(markdownOne.getId());

		Map<String, FileHandle> map = new HashMap<String, FileHandle>();
		map.put(handleOne.getFileName(), handleOne);
		List<String> newIds = new ArrayList<String>();
		newIds.add(handleOne.getId());
		v2RootWiki = v2wikiPageDAO.create(v2RootWiki, map, fileEntity.getId(), ObjectType.ENTITY, newIds);

		// Create a child
		v2SubWiki = new V2WikiPage();
		v2SubWiki.setCreatedBy(adminUserIdString);
		v2SubWiki.setModifiedBy(adminUserIdString);
		v2SubWiki.setParentWikiId(v2RootWiki.getId());
		v2SubWiki.setTitle("V2 Sub-wiki-title");
		v2SubWiki.setMarkdownFileHandleId(markdownOne.getId());
		v2SubWiki = v2wikiPageDAO.create(v2SubWiki, new HashMap<String, FileHandle>(), fileEntity.getId(), ObjectType.ENTITY,
				new ArrayList<String>());
	}

	/**
	 * Create the entities used by this test.
	 * 
	 * @throws JSONObjectAdapterException
	 * @throws ServletException
	 * @throws IOException
	 * @throws NotFoundException
	 */
	public void createEntities() throws JSONObjectAdapterException, ServletException, IOException, NotFoundException {
		// Create a project
		project = new Project();
		project.setName("MigrationIntegrationAutowireTest.Project");
		project.setEntityType(Project.class.getName());
		project = serviceProvider.getEntityService().createEntity(adminUserId, project, null, mockRequest);

		// Create a file entity
		fileEntity = new FileEntity();
		fileEntity.setName("MigrationIntegrationAutowireTest.FileEntity");
		fileEntity.setEntityType(FileEntity.class.getName());
		fileEntity.setParentId(project.getId());
		fileEntity.setDataFileHandleId(handleOne.getId());
		fileEntity = serviceProvider.getEntityService().createEntity(adminUserId, fileEntity, activity.getId(), mockRequest);

		// Create a folder to trash
		folderToTrash = new Folder();
		folderToTrash.setName("boundForTheTrashCan");
		folderToTrash.setParentId(project.getId());
		folderToTrash = serviceProvider.getEntityService().createEntity(adminUserId, folderToTrash, null, mockRequest);
		// Send it to the trash can
		serviceProvider.getTrashService().moveToTrash(adminUserId, folderToTrash.getId());
	}

	private AccessRequirement newAccessRequirement() {
		TermsOfUseAccessRequirement dto = new TermsOfUseAccessRequirement();
		dto.setConcreteType(dto.getClass().getName());
		dto.setAccessType(ACCESS_TYPE.DOWNLOAD);
		dto.setTermsOfUse("foo");
		return dto;
	}

	/**
	 * Create the file handles used by this test.
	 * 
	 * @throws NotFoundException
	 */
	public String createFileHandles() throws NotFoundException {
		// Create a file handle
		handleOne = new S3FileHandle();
		handleOne.setCreatedBy(adminUserIdString);
		handleOne.setCreatedOn(new Date());
		handleOne.setBucketName("bucket");
		handleOne.setKey("mainFileKey");
		handleOne.setEtag("etag");
		handleOne.setFileName("foo.bar");
		handleOne = fileMetadataDao.createFile(handleOne);
		// Create markdown content
		markdownOne = new S3FileHandle();
		markdownOne.setCreatedBy(adminUserIdString);
		markdownOne.setCreatedOn(new Date());
		markdownOne.setBucketName("bucket");
		markdownOne.setKey("markdownFileKey");
		markdownOne.setEtag("etag");
		markdownOne.setFileName("markdown1");
		markdownOne = fileMetadataDao.createFile(markdownOne);
		// Create a preview
		preview = new PreviewFileHandle();
		preview.setCreatedBy(adminUserIdString);
		preview.setCreatedOn(new Date());
		preview.setBucketName("bucket");
		preview.setKey("previewFileKey");
		preview.setEtag("etag");
		preview.setFileName("bar.txt");
		preview = fileMetadataDao.createFile(preview);
		// Set two as the preview of one
		fileMetadataDao.setPreviewId(handleOne.getId(), preview.getId());

		return handleOne.getId();
	}

	private void createStorageQuota() {
		storageQuotaManager.setQuotaForUser(adminUserInfo, adminUserInfo, 3000);
	}

	// returns a group for use in a team
	private UserGroup createUserGroups(int index) throws NotFoundException {
		List<String> adder = new ArrayList<String>();

		// Make one group
		UserGroup parentGroup = new UserGroup();
		parentGroup.setIsIndividual(false);
		parentGroup.setId(userGroupDAO.create(parentGroup).toString());

		// Make two users
		UserGroup parentUser = new UserGroup();
		parentUser.setIsIndividual(true);
		parentUser.setId(userGroupDAO.create(parentUser).toString());

		UserGroup siblingUser = new UserGroup();
		siblingUser.setIsIndividual(true);
		siblingUser.setId(userGroupDAO.create(siblingUser).toString());

		// Nest one group and two users within the parent group
		adder.add(parentUser.getId());
		adder.add(siblingUser.getId());
		groupMembersDAO.addMembers(parentGroup.getId(), adder);

		return parentGroup;
	}

	private void createCredentials(UserGroup group) throws Exception {
		Long principalId = Long.parseLong(group.getId());
		authDAO.changePassword(principalId, "ThisIsMySuperSecurePassword");
		authDAO.changeSecretKey(principalId);
		authDAO.changeSessionToken(principalId, null, DomainType.SYNAPSE);
	}
	
	private void createSessionToken(UserGroup group) throws Exception {
		DBOSessionToken token = new DBOSessionToken();
		token.setDomain(DomainType.SYNAPSE);
		token.setPrincipalId(Long.parseLong(group.getId()));
		token.setSessionToken(UUID.randomUUID().toString());
		token.setValidatedOn(new Date());
		basicDao.createOrUpdate(token);
	}
	
	private void createTermsOfUseAgreement(UserGroup group) throws Exception {
		DBOTermsOfUseAgreement tou = new DBOTermsOfUseAgreement();
		tou.setPrincipalId(Long.parseLong(group.getId()));
		tou.setAgreesToTermsOfUse(Boolean.TRUE);
		tou.setDomain(DomainType.SYNAPSE);
		basicDao.createNew(tou);
	}

	@SuppressWarnings("serial")
	private void createMessages(final UserGroup group, String fileHandleId) {
		MessageToUser dto = new MessageToUser();
		// Note: ID is auto generated
		dto.setCreatedBy(group.getId());
		dto.setFileHandleId(fileHandleId);
		// Note: CreatedOn is set by the DAO
		dto.setSubject("See you on the other side?");
		dto.setRecipients(new HashSet<String>() {
			{
				add(group.getId());
			}
		});
		dto.setInReplyTo(null);
		// Note: InReplyToRoot is calculated by the DAO

		dto = messageDAO.createMessage(dto);

		messageDAO.createMessageStatus_NewTransaction(dto.getId(), group.getId(), null);

		Comment dto2 = new Comment();
		dto2.setCreatedBy(group.getId());
		dto2.setFileHandleId(fileHandleId);
		dto2.setTargetId("1337");
		dto2.setTargetType(ObjectType.ENTITY);
		commentDAO.createComment(dto2);
	}

	private void createTeamsRequestsAndInvitations(UserGroup group) {
		String otherUserId = BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId().toString();

		Team team = new Team();
		team.setId(group.getId());
		team.setName(UUID.randomUUID().toString());
		team.setDescription("test team");
		teamDAO.create(team);

		// create a MembershipRqstSubmission
		MembershipRqstSubmission mrs = new MembershipRqstSubmission();
		Date createdOn = new Date();
		Date expiresOn = new Date();
		mrs.setCreatedOn(createdOn);
		mrs.setExpiresOn(expiresOn);
		mrs.setMessage("Please let me join the team.");
		mrs.setTeamId("" + group.getId());
		// need another valid user group
		mrs.setUserId(otherUserId);
		membershipRqstSubmissionDAO.create(mrs);

		// create a MembershipInvtnSubmission
		MembershipInvtnSubmission mis = new MembershipInvtnSubmission();
		mis.setCreatedOn(createdOn);
		mis.setExpiresOn(expiresOn);
		mis.setMessage("Please join the team.");
		mis.setTeamId("" + group.getId());

		// need another valid user group
		mis.setInviteeId(otherUserId);

		membershipInvtnSubmissionDAO.create(mis);
	}

	private void createCommunity(UserGroup group) throws Exception {
		Team team = new Team();
		team.setId(group.getId());
		team.setName(UUID.randomUUID().toString());
		team.setDescription("test team");
		team = teamDAO.create(team);

		// Create a community
		community = new Community();
		community.setName("MigrationIntegrationAutowireTest.Community");
		community.setEntityType(Community.class.getName());
		community.setTeamId(team.getId());
		community = serviceProvider.getEntityService().createEntity(adminUserId, community, null, mockRequest);

		communityTeamDAO.create(KeyFactory.stringToKey(community.getId()), Long.parseLong(team.getId()));
	}

	private void createParticipantData(UserGroup sampleGroup) throws Exception {
		Long participantId = Long.parseLong(sampleGroup.getId()) ^ -1L;
		bridgeParticipantDAO.create(participantId);
		bridgeUserParticipantMappingDAO.setParticipantIdsForUser(Long.parseLong(sampleGroup.getId()),
				Collections.<ParticipantDataId> singletonList(new ParticipantDataId(participantId)));
		ParticipantDataDescriptor participantDataDescriptor = new ParticipantDataDescriptor();
		participantDataDescriptor.setName(participantId.toString() + "desc");
		participantDataDescriptor.setRepeatType(ParticipantDataRepeatType.ALWAYS);
		participantDataDescriptor.setRepeatFrequency("0 0 4 * * ? *");
		participantDataDescriptor = participantDataDescriptorDAO.createParticipantDataDescriptor(participantDataDescriptor);
		ParticipantDataColumnDescriptor participantDataColumnDescriptor = new ParticipantDataColumnDescriptor();
		participantDataColumnDescriptor.setParticipantDataDescriptorId(participantDataDescriptor.getId());
		participantDataColumnDescriptor.setName("a");
		participantDataColumnDescriptor.setColumnType(ParticipantDataColumnType.STRING);
		participantDataDescriptorDAO.createParticipantDataColumnDescriptor(participantDataColumnDescriptor);
		ParticipantDataColumnDescriptor participantDataColumnDescriptor2 = new ParticipantDataColumnDescriptor();
		participantDataColumnDescriptor2.setParticipantDataDescriptorId(participantDataDescriptor.getId());
		participantDataColumnDescriptor2.setName("b");
		participantDataColumnDescriptor2.setColumnType(ParticipantDataColumnType.STRING);
		participantDataDescriptorDAO.createParticipantDataColumnDescriptor(participantDataColumnDescriptor2);
		ParticipantDataRow dataRow = new ParticipantDataRow();
		ParticipantDataStringValue stringValue1 = new ParticipantDataStringValue();
		stringValue1.setValue("1");
		ParticipantDataStringValue stringValue2 = new ParticipantDataStringValue();
		stringValue2.setValue("2");
		dataRow.setData(ImmutableMap.<String, ParticipantDataValue> builder().put("a", stringValue1).put("b", stringValue2).build());
		List<ParticipantDataRow> data = Lists.newArrayList(dataRow);
		participantDataDAO.append(new ParticipantDataId(participantId), participantDataDescriptor.getId(), data,
				Lists.newArrayList(participantDataColumnDescriptor, participantDataColumnDescriptor2));
		ParticipantDataStatus status = new ParticipantDataStatus();
		status.setParticipantDataDescriptorId(participantDataDescriptor.getId());
		status.setLastEntryComplete(false);
		status.setLastPrompted(new Date());
		status.setLastStarted(new Date());
		participantDataStatusDAO.update(Collections.<ParticipantDataStatus> singletonList(status), ImmutableMap
				.<String, ParticipantDataId> builder().put(participantDataDescriptor.getId(), new ParticipantDataId(participantId)).build());
	}

	@After
	public void after() throws Exception {
		// to cleanup for this test we delete all in the database
		resetDatabase();
	}
	
	// test that if we create a group with members, back it up, 
	// add members, and restore, the extra members are removed
	// (This was broken in PLFM-2757)
	@Test
	public void testCertifiedUsersGroupMigration() throws Exception {
		String groupId = BOOTSTRAP_PRINCIPAL.CERTIFIED_USERS.getPrincipalId().toString();
		List<UserGroup> members = groupMembersDAO.getMembers(groupId);
		
		List<BackupInfo> backupList = backupAllOfType(MigrationType.PRINCIPAL);
		
		// add new member(s)
		UserGroup yetAnotherUser = new UserGroup();
		yetAnotherUser.setIsIndividual(true);
		yetAnotherUser.setId(userGroupDAO.create(yetAnotherUser).toString());
		groupMembersDAO.addMembers(groupId, Collections.singletonList(yetAnotherUser.getId()));

		// membership is different because new user has been added
		assertFalse(members.equals(groupMembersDAO.getMembers(groupId)));
		
		// Now restore all of the data
		for (BackupInfo info : backupList) {
			String fileName = info.getFileName();
			assertNotNull("Did not find a backup file name for type: " + info.getType(), fileName);
			restoreFromBackup(info.getType(), fileName);
		}
		
		// should be back to normal
		assertEquals(members, groupMembersDAO.getMembers(groupId));
	}

	/**
	 * This is the actual test. The rest of the class is setup and tear down.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testRoundTrip() throws Exception {
		// Get the list of primary types
		MigrationTypeList primaryTypesList = entityServletHelper.getPrimaryMigrationTypes(migrationUserId);
		assertNotNull(primaryTypesList);
		assertNotNull(primaryTypesList.getList());
		assertTrue(primaryTypesList.getList().size() > 0);
		// Get the counts before we start
		MigrationTypeCounts startCounts = entityServletHelper.getMigrationTypeCounts(migrationUserId);
		validateStartingCount(startCounts);

		// This test will backup all data, delete it, then restore it.
		List<BackupInfo> backupList = new ArrayList<BackupInfo>();
		for (MigrationType type : primaryTypesList.getList()) {
			// Backup each type
			backupList.addAll(backupAllOfType(type));
		}

		// Now delete all data in reverse order
		for (int i = primaryTypesList.getList().size() - 1; i >= 0; i--) {
			MigrationType type = primaryTypesList.getList().get(i);
			deleteAllOfType(type);
		}

		// After deleting, the counts should be 0 except for a few special cases
		MigrationTypeCounts afterDeleteCounts = entityServletHelper.getMigrationTypeCounts(migrationUserId);
		assertNotNull(afterDeleteCounts);
		assertNotNull(afterDeleteCounts.getList());

		for (int i = 0; i < afterDeleteCounts.getList().size(); i++) {
			MigrationTypeCount afterDelete = afterDeleteCounts.getList().get(i);

			// Special cases for the not-deleted migration admin
			if (afterDelete.getType() == MigrationType.PRINCIPAL) {
				assertEquals("There should be 4 UserGroups remaining after the delete: " + BOOTSTRAP_PRINCIPAL.MIGRATION_USER + ", "
						+ "Administrators" + ", " + BOOTSTRAP_PRINCIPAL.PUBLIC_GROUP + ", and "
						+ BOOTSTRAP_PRINCIPAL.AUTHENTICATED_USERS_GROUP, new Long(4), afterDelete.getCount());
			} else if (afterDelete.getType() == MigrationType.CREDENTIAL) {
				assertEquals("Counts do not match for: " + afterDelete.getType().name(), new Long(1), afterDelete.getCount());

			} else {
				assertEquals("Counts are non-zero for: " + afterDelete.getType().name(), new Long(0), afterDelete.getCount());
			}
		}

		// Now restore all of the data
		for (BackupInfo info : backupList) {
			String fileName = info.getFileName();
			assertNotNull("Did not find a backup file name for type: " + info.getType(), fileName);
			restoreFromBackup(info.getType(), fileName);
		}

		// The counts should all be back
		MigrationTypeCounts finalCounts = entityServletHelper.getMigrationTypeCounts(migrationUserId);
		for (int i = 1; i < finalCounts.getList().size(); i++) {
			MigrationTypeCount startCount = startCounts.getList().get(i);
			MigrationTypeCount afterRestore = finalCounts.getList().get(i);
			assertEquals("Count for " + startCount.getType().name() + " does not match", startCount.getCount(), afterRestore.getCount());
		}
	}

	private static class BackupInfo {
		MigrationType type;
		String fileName;

		public BackupInfo(MigrationType type, String fileName) {
			super();
			this.type = type;
			this.fileName = fileName;
		}

		public MigrationType getType() {
			return type;
		}

		public String getFileName() {
			return fileName;
		}
	}

	/**
	 * There must be at least one object for every type of migratable object.
	 * 
	 * @param startCounts
	 */
	private void validateStartingCount(MigrationTypeCounts startCounts) {
		assertNotNull(startCounts);
		assertNotNull(startCounts.getList());
		List<MigrationType> typesToMigrate = new LinkedList<MigrationType>();
		for (MigrationType tm : MigrationType.values()) {
			if (migrationManager.isMigrationTypeUsed(migrationUserInfo, tm)) {
				typesToMigrate.add(tm);
			}
		}
		assertEquals(
				"This test requires at least one object to exist for each MigrationType.  Please create a new object of the new MigrationType in the before() method of this test.",
				typesToMigrate.size(), startCounts.getList().size());
		for (MigrationTypeCount count : startCounts.getList()) {
			assertTrue("This test requires at least one object to exist for each MigrationType.  Please create a new object of type: "
					+ count.getType() + " in the before() method of this test.", count.getCount() > 0);
		}
	}

	/**
	 * Extract the filename from the full url.
	 * 
	 * @param fullUrl
	 * @return
	 */
	public String getFileNameFromUrl(String fullUrl) {
		;
		int index = fullUrl.lastIndexOf("/");
		return fullUrl.substring(index + 1, fullUrl.length());
	}

	/**
	 * Backup all data
	 * 
	 * @param type
	 * @return
	 * @throws Exception
	 */
	private List<BackupInfo> backupAllOfType(MigrationType type) throws Exception {
		RowMetadataResult list = entityServletHelper.getRowMetadata(migrationUserId, type, Long.MAX_VALUE, 0);
		if (list == null)
			return null;
		// Backup batches by their level in the tree
		ListBucketProvider provider = new ListBucketProvider();
		MigrationUtils.bucketByTreeLevel(list.getList().iterator(), provider);
		List<BackupInfo> result = new ArrayList<BackupInfo>();
		List<List<Long>> listOfBuckets = provider.getListOfBuckets();
		for (List<Long> batch : listOfBuckets) {
			if (batch.size() > 0) {
				String fileName = backup(type, batch);
				result.add(new BackupInfo(type, fileName));
			}
		}
		return result;
	}

	private String backup(MigrationType type, List<Long> tobackup) throws Exception {
		// Start the backup job
		IdList ids = new IdList();
		ids.setList(tobackup);
		BackupRestoreStatus status = entityServletHelper.startBackup(migrationUserId, type, ids);
		// wait for it..
		waitForDaemon(status);
		status = entityServletHelper.getBackupRestoreStatus(migrationUserId, status.getId());
		assertNotNull(status.getBackupUrl());
		return getFileNameFromUrl(status.getBackupUrl());
	}

	private void restoreFromBackup(MigrationType type, String fileName) throws Exception {
		RestoreSubmission sub = new RestoreSubmission();
		sub.setFileName(fileName);
		BackupRestoreStatus status = entityServletHelper.startRestore(migrationUserId, type, sub);
		// wait for it
		waitForDaemon(status);
	}

	/**
	 * Delete all data for a type.
	 * 
	 * @param type
	 * @throws ServletException
	 * @throws IOException
	 * @throws JSONObjectAdapterException
	 */
	private void deleteAllOfType(MigrationType type) throws Exception {
		IdList idList = getIdListOfAllOfType(type);
		if (idList == null)
			return;
		MigrationTypeCount result = entityServletHelper.deleteMigrationType(migrationUserId, type, idList);
		System.out.println("Deleted: " + result);
	}

	/**
	 * List all of the IDs for a type.
	 * 
	 * @param type
	 * @return
	 * @throws ServletException
	 * @throws IOException
	 * @throws JSONObjectAdapterException
	 */
	private IdList getIdListOfAllOfType(MigrationType type) throws Exception {
		RowMetadataResult list = entityServletHelper.getRowMetadata(migrationUserId, type, Long.MAX_VALUE, 0);
		if (list.getTotalCount() < 1)
			return null;
		// Create the backup list
		List<Long> toBackup = new LinkedList<Long>();
		for (RowMetadata row : list.getList()) {
			toBackup.add(row.getId());
		}
		IdList idList = new IdList();
		idList.setList(toBackup);
		return idList;
	}

	/**
	 * Wait for a deamon to process a a job.
	 * 
	 * @param status
	 * @throws InterruptedException
	 * @throws JSONObjectAdapterException
	 * @throws IOException
	 * @throws ServletException
	 */
	private void waitForDaemon(BackupRestoreStatus status) throws Exception {
		long start = System.currentTimeMillis();
		while (DaemonStatus.COMPLETED != status.getStatus()) {
			assertFalse("Daemon failed " + status.getErrorDetails(), DaemonStatus.FAILED == status.getStatus());
			System.out.println("Waiting for backup/restore daemon.  Message: " + status.getProgresssMessage());
			Thread.sleep(1000);
			long elapse = System.currentTimeMillis() - start;
			assertTrue("Timed out waiting for a backup/restore daemon", elapse < MAX_WAIT_MS);
			status = entityServletHelper.getBackupRestoreStatus(migrationUserId, status.getId());
		}
	}

}
