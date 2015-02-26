package org.sagebionetworks.projectstats.worker;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.asynchronous.workers.sqs.MessageUtils;
import org.sagebionetworks.asynchronous.workers.sqs.Worker;
import org.sagebionetworks.asynchronous.workers.sqs.WorkerProgress;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.PaginatedResultsUtil;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.ProjectHeader;
import org.sagebionetworks.repo.model.ProjectListSortColumn;
import org.sagebionetworks.repo.model.ProjectListType;
import org.sagebionetworks.repo.model.ProjectStat;
import org.sagebionetworks.repo.model.ProjectStatsDAO;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.TeamDAO;
import org.sagebionetworks.repo.model.TeamMember;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.entity.query.SortDirection;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.message.AclModificationMessage;
import org.sagebionetworks.repo.model.message.DefaultModificationMessage;
import org.sagebionetworks.repo.model.message.ModificationMessage;
import org.sagebionetworks.repo.model.message.TeamModificationMessage;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.TransientDataAccessException;

import com.amazonaws.services.sqs.model.Message;
import com.google.common.base.Function;

/**
 * This worker will stream the results of a table SQL query to a local CSV file and upload the file to S3 as a
 * FileHandle.
 * 
 */
public class ProjectStatsWorker implements Worker {

	private static final long BATCH_SIZE = 200;

	static private Logger log = LogManager.getLogger(ProjectStatsWorker.class);
	private List<Message> messages;
	private WorkerProgress workerProgress;
	private EntityType projectEntityType = EntityType.getNodeTypeForClass(Project.class);

	@Autowired
	private ProjectStatsDAO projectStatsDao;

	@Autowired
	private NodeDAO nodeDao;

	@Autowired
	private UserManager userManager;

	@Autowired
	private TeamDAO teamDAO;

	@Override
	public void setMessages(List<Message> messages) {
		this.messages = messages;
	}

	@Override
	public void setWorkerProgress(WorkerProgress workerProgress) {
		this.workerProgress = workerProgress;
	}

	@Override
	public List<Message> call() throws Exception {
		List<Message> toDelete = new LinkedList<Message>();
		for (Message message : messages) {
			try {
				Message returned = processMessage(message);
				if (returned != null) {
					toDelete.add(returned);
				}
			}catch(NotFoundException e){
				// entity no longer exists. Common case, so no reason to log an error
				toDelete.add(message);
			} catch (Throwable e) {
				// Treat unknown errors as unrecoverable and return them
				toDelete.add(message);
				log.error("Worker Failed", e);
			}
		}
		return toDelete;
	}

	private Message processMessage(Message message) throws Throwable {
		try {
			ModificationMessage modificationMessage = extractStatus(message);

			if (modificationMessage instanceof TeamModificationMessage) {
				final TeamModificationMessage teamModificationMessage = (TeamModificationMessage) modificationMessage;
				if (teamModificationMessage.getObjectType() == ObjectType.PRINCIPAL) {
					// for a team modification message, we must update all projects for the member, that the team has
					// access to
					Team team = teamDAO.get(teamModificationMessage.getObjectId());

					updateProjectStats(team, teamModificationMessage.getTimestamp(), teamModificationMessage.getMemberId());
				} else if (teamModificationMessage.getObjectType() == ObjectType.TEAM) {
					// for a team modification message, we must update all projects for the member, that the team has
					// access to
					final Team team = teamDAO.get(teamModificationMessage.getObjectId());

					applyToAllTeamMembers(team, new Function<long[], Void>() {
						@Override
						public Void apply(long[] members) {
							updateProjectStats(team, teamModificationMessage.getTimestamp(), members);
							return null;
						}
					});
				} else {
					throw new IllegalArgumentException("cannot handle team modification type " + teamModificationMessage.getObjectType());
				}
			} else if (modificationMessage instanceof AclModificationMessage) {
				final AclModificationMessage aclModificationMessage = (AclModificationMessage) modificationMessage;
				if (aclModificationMessage.getObjectType() == ObjectType.ENTITY) {
					final Long projectId = getProjectIdFromEntityId(aclModificationMessage.getObjectId());
					if (projectId != null) {
						try {
							// for a team principal added, we must update for all users in that team and that project
							final Team team = teamDAO.get(aclModificationMessage.getPrincipalId().toString());

							applyToAllTeamMembers(team, new Function<long[], Void>() {
								@Override
								public Void apply(long[] members) {
									for (long member : members) {
										ProjectStat projectStat = new ProjectStat(projectId, member, aclModificationMessage.getTimestamp());
										projectStatsDao.update(projectStat);
									}
									return null;
								}
							});
						} catch (NotFoundException e) {
							// it's a user, not a team. Add the one entry for this project
							ProjectStat projectStat = new ProjectStat(projectId, aclModificationMessage.getPrincipalId(),
									aclModificationMessage.getTimestamp());
							projectStatsDao.update(projectStat);
						}
					}
				} else {
					// ignore other types of objects
				}
			} else if (modificationMessage instanceof DefaultModificationMessage) {
				Long projectId = null;
				if (modificationMessage.getObjectType() == ObjectType.ENTITY) {
					projectId = getProjectIdFromEntityId(modificationMessage.getObjectId());
				} else {
					throw new IllegalArgumentException("cannot handle type " + modificationMessage.getObjectType());
				}

				if (projectId != null) {
					ProjectStat projectStat = new ProjectStat(projectId, modificationMessage.getUserId(), modificationMessage.getTimestamp());
					projectStatsDao.update(projectStat);
				}
			} else {
				throw new IllegalArgumentException("cannot modification type " + modificationMessage.getObjectType());
			}
			return message;
		} catch (TransientDataAccessException e) {
			return null;
		}
	}

	private void applyToAllTeamMembers(Team team, Function<long[], Void> updater) {
		// we batch this just in case.
		long membersCount = teamDAO.getMembersCount(team.getId());
		for (long start = 0; start < membersCount; start += BATCH_SIZE) {
			List<TeamMember> membersInRange = teamDAO.getMembersInRange(team.getId(), start + BATCH_SIZE, start);
			long members[] = new long[membersInRange.size()];
			for (int i = 0; i < members.length; i++) {
				members[i] = KeyFactory.stringToKey(membersInRange.get(i).getMember().getOwnerId());
			}
			updater.apply(members);
		}
	}

	private void updateProjectStats(final Team team, Date timestamp, long... members) {
		Iterable<ProjectHeader> projectHeaders = PaginatedResultsUtil.getPaginatedResultsIterable(
				new PaginatedResultsUtil.Paginator<ProjectHeader>() {
					@Override
					public PaginatedResults<ProjectHeader> getBatch(long limit, long offset) {
						// we must call this as admin, because we want to see all headers, regardless of
						// access
						UserInfo adminUser = new UserInfo(true, BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
						return nodeDao.getProjectHeaders(adminUser, adminUser, team, ProjectListType.TEAM_PROJECTS,
								ProjectListSortColumn.PROJECT_NAME, SortDirection.ASC, (int) limit, (int) offset);
					}
				}, 500);

		for (ProjectHeader projectHeader : projectHeaders) {
			for (long member : members) {
				ProjectStat projectStat = new ProjectStat(KeyFactory.stringToKey(projectHeader.getId()), member, timestamp);
				projectStatsDao.update(projectStat);
			}
		}
	}

	private Long getProjectIdFromEntityId(String entityId) throws NotFoundException {
		Node node = nodeDao.getNode(entityId);
		return node.getProjectId() == null ? null : KeyFactory.stringToKey(node.getProjectId());
	}

	/**
	 * Extract the AsynchUploadRequestBody from the message.
	 * 
	 * @param message
	 * @return
	 * @throws JSONObjectAdapterException
	 */
	ModificationMessage extractStatus(Message message) throws JSONObjectAdapterException {
		ValidateArgument.required(message, "message");

		ModificationMessage modificationMessage = MessageUtils.extractMessageBody(message, ModificationMessage.class);

		ValidateArgument.required(modificationMessage.getUserId(), "modificationMessage.modificationInfo.userId");
		ValidateArgument.required(modificationMessage.getTimestamp(), "modificationMessage.timestamp");

		return modificationMessage;
	}
}
