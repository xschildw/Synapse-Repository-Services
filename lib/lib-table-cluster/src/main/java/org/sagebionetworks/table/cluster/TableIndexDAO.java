package org.sagebionetworks.table.cluster;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.model.dao.table.RowHandler;
import org.sagebionetworks.repo.model.table.ColumnChangeDetails;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.EntityDTO;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.repo.model.table.ViewType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionCallback;

/**
 * This is an abstraction for table index CRUD operations.
 * @author John
 *
 */
public interface TableIndexDAO {
	
	/**
	 * Create a table with the given name if it does not exist.
	 * @param tableId The ID of the table.
	 */
	public void createTableIfDoesNotExist(String tableId);
	
	/**
	 * Alter the given table as needed. The table will be changed
	 * according to the passed list of column changes.  This includes,
	 * additions, deletions, and updates.
	 * 
	 * @param tableId
	 * @param changes
	 * @param alterTemp When true the temporary table will be altered.  When false the original table will be altered.
	 * @return True if the table was altered. False if the table was not changed.
	 */
	public boolean alterTableAsNeeded(String tableId, List<ColumnChangeDetails> changes, boolean alterTemp);
	
	/**
	 * 
	 * @param connection
	 * @param tableId
	 * @return
	 */
	public boolean deleteTable(String tableId); 
	
	/**
	 * Create or update the rows passed in the given RowSet.
	 * 
	 * Note: The passed RowSet is not required to match the current schema.
	 * Columns in the Rowset that are not part of the current schema will be ignored.
	 * Columns in the current schema that are not part of the RowSet will be set to
	 * the default value of the column.
	 * @param connection
	 * @param rowset
	 * @return
	 */
	void createOrUpdateOrDeleteRows(RowSet rowset, List<ColumnModel> currentSchema);
	
	/**
	 * Query a RowSet from the table.
	 * @param query
	 * @return
	 */
	public RowSet query(ProgressCallback<Void> callback, SqlQuery query);
	
	/**
	 * Run a simple count query.
	 * @param sql
	 * @param parameters
	 * @return
	 */
	public Long countQuery(String sql, Map<String, Object> parameters);
	
	/**
	 * Provides the means to stream over query results without keeping the row data in memory.
	 * 
	 * @param query
	 * @param handler
	 * @return
	 */
	public boolean queryAsStream(ProgressCallback<Void> callback, SqlQuery query, RowHandler handler);
	
	/**
	 * Get the row count for this table.
	 * 
	 * @param tableId
	 * @return The row count of the table. If the table does not exist then null.
	 */
	public Long getRowCountForTable(String tableId);
	
	/**
	 * Get the max complete version we currently have for this table.
	 * 
	 * @param tableId
	 * @param version the max complete version to remember
	 * @return The max complete version of the table. If the table does not exist then -1L.
	 */
	public Long getMaxCurrentCompleteVersionForTable(String tableId);

	/**
	 * Set the max complete version for this table
	 * 
	 * @param tableId
	 * @param highestVersion
	 */
	public void setMaxCurrentCompleteVersionForTable(String tableId, Long highestVersion);
	
	/**
	 * Set the MD5 hex of the table's current schema.
	 * 
	 * @param tableId
	 * @param schemaMD5Hex
	 */
	public void setCurrentSchemaMD5Hex(String tableId, String schemaMD5Hex);
	
	/**
	 * Get the MD5 hex of the table's current schema.
	 * @param tableId
	 * @return
	 */
	public String getCurrentSchemaMD5Hex(String tableId);

	/**
	 * Delete all of the secondary tables used for an index if they exist.
	 * 
	 * @param tableId
	 */
	public void deleteSecondayTables(String tableId);
	
	/**
	 * Create all of the secondary tables used for an index if they do not exist.
	 * @param tableId
	 */
	public void createSecondaryTables(String tableId);
	
	/**
	 * Get the connection
	 * @return
	 */
	public JdbcTemplate getConnection();

	/**
	 * run calls within a read transaction
	 * 
	 * @param callable
	 * @return
	 */
	public <T> T executeInReadTransaction(TransactionCallback<T> callable);
	
	/**
	 * Run the passed callable within a write transaction.
	 * @param callable
	 * @return
	 */
	public <T> T executeInWriteTransaction(TransactionCallback<T> callable);

	/**
	 * Apply the passed set of file handle Ids to the given table index.
	 * 
	 * @param tableId
	 * @param fileHandleIds
	 */
	public void applyFileHandleIdsToTable(String tableId,
			Set<Long> fileHandleIds);
	
	/**
	 * Given a set of FileHandleIds and a talbeId, get the sub-set of
	 * FileHandleIds that are actually associated with the table.
	 * @param toTest
	 * @param objectId
	 * @return
	 */
	public Set<Long> getFileHandleIdsAssociatedWithTable(
			Set<Long> toTest, String tableId);
	
	/**
	 * Does the state of the index match the given data?
	 * 
	 * @param tableId
	 * @param versionNumber
	 * @param schemaMD5Hex
	 * @return
	 */
	public boolean doesIndexStateMatch(String tableId, long versionNumber, String schemaMD5Hex);

	/**
	 * Get the distinct Long values for a given column ID.
	 * 
	 * @param id
	 * @return
	 */
	public Set<Long> getDistinctLongValues(String tableId, String columnIds);

	/**
	 * Truncate all of the data in the given table.
	 * 
	 * @param tableId
	 */
	public void truncateTable(String tableId);
	
	
	/**
	 * Get information about each column of a database table.
	 * 
	 * @param tableId
	 * @return
	 */
	public List<DatabaseColumnInfo> getDatabaseInfo(String tableId);
	
	/**
	 * Provide the cardinality for the given columns and table.
	 * 
	 * Note: A single query will be executed, and the results added to the passed info list.
	 * 
	 * @param list
	 * @param tableId
	 */
	public void provideCardinality(List<DatabaseColumnInfo> list, String tableId);
	
	/**
	 * Provide the index name for each column in the table.
	 * @param list
	 * @param tableId
	 */
	public void provideIndexName(List<DatabaseColumnInfo> list, String tableId);
	
	
	/**
	 * The provided column data is used to optimize the indices on the given
	 * table. Indices are added until either all columns have an index or the
	 * maximum number of indices per table is reached. When a table has more
	 * columns than the maximum number of indices, indices are assigned to
	 * columns with higher cardinality before columns with low cardinality.
	 * 
	 * @param list
	 *            The current column information of this table used for the
	 *            optimization.
	 * @param tableId
	 *            The table to optimize.
	 * @param maxNumberOfIndex
	 *            The maximum number of indices allowed on a single table.
	 */
	public void optimizeTableIndices(List<DatabaseColumnInfo> list, String tableId, int maxNumberOfIndex);

	/**
	 * Create a temporary table like the given table.
	 * @param tableId
	 */
	public void createTemporaryTable(String tableId);

	/**
	 * Copy all of the data from the original table to the temporary table.
	 * @param tableId
	 */
	public void copyAllDataToTemporaryTable(String tableId);

	/**
	 * Delete the temporary table associated with the given table.
	 */
	public void deleteTemporaryTable(String tableId);

	/**
	 * Count the rows in the temp table.
	 * @param tableId
	 * @return
	 */
	public long getTempTableCount(String tableId);
	
	/**
	 * Create the entity replication tables if they do not exist.
	 * 
	 */
	void createEntityReplicationTablesIfDoesNotExist();

	/**
	 * Delete all entity data with the given Ids.
	 * @param progressCallback 
	 * 
	 * @param allIds
	 */
	public void deleteEntityData(ProgressCallback<Void> progressCallback, List<Long> allIds);

	/**
	 * Add the given entity data to the index.
	 * 
	 * @param entityDTOs
	 */
	public void addEntityData(ProgressCallback<Void> progressCallback, List<EntityDTO> entityDTOs);
	
	/**
	 * Get the entity DTO for a given entity ID.
	 * @param entityId
	 * @return
	 */
	public EntityDTO getEntityData(Long entityId);

	/**
	 * Given a container scope calculate the CRC32 of the entity replication table on 'id-etag'.
	 * @param viewType 
	 * 
	 * @param allContainersInScope
	 * @return
	 */
	public long calculateCRC32ofEntityReplicationScope(
			ViewType viewType, Set<Long> allContainersInScope);

	/**
	 * Copy the data from the entity replication tables to the given view's table.
	 * 
	 * @param viewId
	 * @param viewType
	 * @param allContainersInScope
	 * @param currentSchema
	 */
	public void copyEntityReplicationToTable(String viewId, ViewType viewType,
			Set<Long> allContainersInScope, List<ColumnModel> currentSchema);

	/**
	 * Calculate the Cyclic-Redundancy-Check (CRC) of a table view's concatenation
	 * of ROW_ID + ETAG.  Used to determine if a view is synchronized with the
	 * truth.
	 * 
	 * @param viewId
	 * @param etagColumnId The ID of the view's ETAG column.
	 * 
	 * @return
	 */
	long calculateCRC32ofTableView(String viewId, String etagColumnId);

	/**
	 * Save both the current version and schema MD5 for current index.
	 * 
	 * @param tableId
	 * @param viewCRC
	 * @param schemaMD5Hex
	 */
	public void setIndexVersionAndSchemaMD5Hex(String tableId, Long viewCRC,
			String schemaMD5Hex);
}
