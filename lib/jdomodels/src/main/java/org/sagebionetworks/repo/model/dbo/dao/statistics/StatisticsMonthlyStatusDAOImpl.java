package org.sagebionetworks.repo.model.dbo.dao.statistics;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_STATISTICS_MONTHLY_STATUS_MONTH;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_STATISTICS_MONTHLY_STATUS_OBJECT_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_STATISTICS_MONTHLY_STATUS_STATUS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_STATISTICS_MONTHLY_STATUS_LAST_UPDATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_STATISTICS_MONTHLY_STATUS;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import org.sagebionetworks.repo.model.dao.statistics.StatisticsMonthlyStatusDAO;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.statistics.monthly.DBOMonthlyStatisticsStatus;
import org.sagebionetworks.repo.model.statistics.StatisticsObjectType;
import org.sagebionetworks.repo.model.statistics.StatisticsStatus;
import org.sagebionetworks.repo.model.statistics.monthly.StatisticsMonthlyStatus;
import org.sagebionetworks.repo.model.statistics.monthly.StatisticsMonthlyUtils;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.EmptySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

@Repository
public class StatisticsMonthlyStatusDAOImpl implements StatisticsMonthlyStatusDAO {

	private static final String PARAM_LAST_UPDATED_ON = "lastUpdatedOn";
	private static final String PARAM_OBJECT_TYPE = "objectType";
	private static final String PARAM_MONTH = "month";
	private static final String PARAM_STATUS = "status";
	private static final String PARAM_FROM = "from";
	private static final String PARAM_TO = "to";

	private static final String SQL_DELETE_ALL = "DELETE FROM " + TABLE_STATISTICS_MONTHLY_STATUS;

	private static final String SQL_SELECT_IN_RANGE = "SELECT * FROM " + TABLE_STATISTICS_MONTHLY_STATUS + " WHERE "
			+ COL_STATISTICS_MONTHLY_STATUS_OBJECT_TYPE + " = :" + PARAM_OBJECT_TYPE + " AND " + COL_STATISTICS_MONTHLY_STATUS_STATUS
			+ " =:" + PARAM_STATUS + " AND " + COL_STATISTICS_MONTHLY_STATUS_MONTH + " BETWEEN :" + PARAM_FROM + " AND :" + PARAM_TO
			+ " ORDER BY " + COL_STATISTICS_MONTHLY_STATUS_MONTH;

	private static final String SQL_TOUCH = "UPDATE " + TABLE_STATISTICS_MONTHLY_STATUS + " SET "
			+ COL_STATISTICS_MONTHLY_STATUS_LAST_UPDATED_ON + "=:" + PARAM_LAST_UPDATED_ON + " WHERE "
			+ COL_STATISTICS_MONTHLY_STATUS_OBJECT_TYPE + "=:" + PARAM_OBJECT_TYPE + " AND " + COL_STATISTICS_MONTHLY_STATUS_MONTH + "=:"
			+ PARAM_MONTH;

	private static final RowMapper<DBOMonthlyStatisticsStatus> DBO_MAPPER = new DBOMonthlyStatisticsStatus().getTableMapping();

	private static final RowMapper<StatisticsMonthlyStatus> ROW_MAPPER = new RowMapper<StatisticsMonthlyStatus>() {
		@Override
		public StatisticsMonthlyStatus mapRow(ResultSet rs, int rowNum) throws SQLException {
			return map(DBO_MAPPER.mapRow(rs, rowNum));
		}
	};

	private DBOBasicDao basicDao;
	private NamedParameterJdbcTemplate jdbcTemplate;

	@Autowired
	public StatisticsMonthlyStatusDAOImpl(DBOBasicDao basicDao, NamedParameterJdbcTemplate jdbcTemplate) {
		this.basicDao = basicDao;
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	@WriteTransaction
	public StatisticsMonthlyStatus setAvailable(StatisticsObjectType objectType, YearMonth month) {
		return createOrUpdate(objectType, month, StatisticsStatus.AVAILABLE, null, null, null);
	}

	@Override
	@WriteTransaction
	public StatisticsMonthlyStatus setProcessingFailed(StatisticsObjectType objectType, YearMonth month, String errorMessage,
			String errorDetails) {
		return createOrUpdate(objectType, month, StatisticsStatus.PROCESSING_FAILED, null, errorMessage, errorDetails);
	}

	@Override
	@WriteTransaction
	public StatisticsMonthlyStatus setProcessing(StatisticsObjectType objectType, YearMonth month) {
		return createOrUpdate(objectType, month, StatisticsStatus.PROCESSING, System.currentTimeMillis(), null, null);
	}

	@Override
	@WriteTransaction
	public boolean touch(StatisticsObjectType objectType, YearMonth month) {
		MapSqlParameterSource params = getPrimaryKeyParams(objectType, month);
		params.addValue(PARAM_LAST_UPDATED_ON, System.currentTimeMillis());
		return jdbcTemplate.update(SQL_TOUCH, params) > 0;
	}

	@Override
	public Optional<StatisticsMonthlyStatus> getStatus(StatisticsObjectType objectType, YearMonth month) {
		return getStatus(objectType, month, false);
	}

	@Override
	public Optional<StatisticsMonthlyStatus> getStatusForUpdate(StatisticsObjectType objectType, YearMonth month) {
		return getStatus(objectType, month, true);
	}

	@Override
	public List<StatisticsMonthlyStatus> getAvailableStatusInRange(StatisticsObjectType objectType, YearMonth from, YearMonth to) {
		ValidateArgument.required(objectType, "objectType");
		ValidateArgument.required(from, "from");
		ValidateArgument.required(to, "to");
		ValidateArgument.requirement(from.isBefore(to), "The start of the range should be before the end");

		MapSqlParameterSource params = new MapSqlParameterSource();

		params.addValue(PARAM_OBJECT_TYPE, objectType.toString());
		params.addValue(PARAM_STATUS, StatisticsStatus.AVAILABLE.toString());
		params.addValue(PARAM_FROM, StatisticsMonthlyUtils.toDate(from));
		params.addValue(PARAM_TO, StatisticsMonthlyUtils.toDate(to));

		return jdbcTemplate.query(SQL_SELECT_IN_RANGE, params, ROW_MAPPER);
	}

	@Override
	@WriteTransaction
	public void clear() {
		jdbcTemplate.update(SQL_DELETE_ALL, EmptySqlParameterSource.INSTANCE);
	}

	private Optional<StatisticsMonthlyStatus> getStatus(StatisticsObjectType objectType, YearMonth month, boolean forUpdate) {
		ValidateArgument.required(objectType, "objectType");
		ValidateArgument.required(month, "month");

		SqlParameterSource params = getPrimaryKeyParams(objectType, month);

		DBOMonthlyStatisticsStatus dbo;

		try {
			if (forUpdate) {
				dbo = basicDao.getObjectByPrimaryKeyWithUpdateLock(DBOMonthlyStatisticsStatus.class, params);
			} else {
				dbo = basicDao.getObjectByPrimaryKey(DBOMonthlyStatisticsStatus.class, params);
			}
		} catch (NotFoundException e) {
			return Optional.empty();
		}

		return Optional.of(map(dbo));
	}

	private StatisticsMonthlyStatus createOrUpdate(StatisticsObjectType objectType, YearMonth month, StatisticsStatus status,
			Long lastStartedOn, String errorMessage, String errorDetails) {
		ValidateArgument.required(objectType, "objectType");
		ValidateArgument.required(month, "month");
		ValidateArgument.required(status, "status");

		SqlParameterSource params = getPrimaryKeyParams(objectType, month);

		DBOMonthlyStatisticsStatus dbo;

		try {
			dbo = basicDao.getObjectByPrimaryKeyWithUpdateLock(DBOMonthlyStatisticsStatus.class, params);
		} catch (NotFoundException e) {
			dbo = new DBOMonthlyStatisticsStatus();
		}

		dbo.setObjectType(objectType.toString());
		dbo.setMonth(StatisticsMonthlyUtils.toDate(month));
		dbo.setStatus(status.toString());
		dbo.setLastUpdatedOn(System.currentTimeMillis());
		dbo.setErrorMessage(StatisticsMonthlyUtils.encodeErrorMessage(errorMessage, DBOMonthlyStatisticsStatus.MAX_ERROR_MESSAGE_CHARS));
		dbo.setErrorDetails(StatisticsMonthlyUtils.encodeErrorDetails(errorDetails));

		// Avoid overriding the previous timestamp
		if (lastStartedOn != null) {
			dbo.setLastStartedOn(lastStartedOn);
		}

		return map(basicDao.createOrUpdate(dbo));
	}

	private MapSqlParameterSource getPrimaryKeyParams(StatisticsObjectType objectType, YearMonth month) {
		MapSqlParameterSource params = new MapSqlParameterSource();

		params.addValue(PARAM_OBJECT_TYPE, objectType.toString());
		params.addValue(PARAM_MONTH, StatisticsMonthlyUtils.toDate(month));

		return params;
	}

	private static StatisticsMonthlyStatus map(DBOMonthlyStatisticsStatus dbo) {
		StatisticsMonthlyStatus dto = new StatisticsMonthlyStatus();

		dto.setObjectType(StatisticsObjectType.valueOf(dbo.getObjectType()));
		dto.setStatus(StatisticsStatus.valueOf(dbo.getStatus()));
		dto.setMonth(YearMonth.from(dbo.getMonth()));
		dto.setLastStartedOn(dbo.getLastStartedOn());
		dto.setLastUpdatedOn(dbo.getLastUpdatedOn());

		return dto;

	}

}