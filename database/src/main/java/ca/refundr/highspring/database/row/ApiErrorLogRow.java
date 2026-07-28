package ca.refundr.highspring.database.row;

import ca.refundr.highspring.database.tables.Tables;
import com.google.common.base.Preconditions;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;

import java.sql.Connection;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

/**
 * A serious server failure saved so admins (and developers) can review it later.
 */
public final class ApiErrorLogRow {

	private final long id;
	private final String level;
	private final String loggerName;
	private final String message;
	private final String stackTrace;
	private final String requestMethod;
	private final String requestPath;
	private final UUID userId;
	private final Instant createdAt;

	public ApiErrorLogRow(long id, String level, String loggerName, String message, String stackTrace,
		String requestMethod, String requestPath, UUID userId, Instant createdAt) {
		this.id = id;
		this.level = level;
		this.loggerName = loggerName;
		this.message = message;
		this.stackTrace = stackTrace;
		this.requestMethod = requestMethod;
		this.requestPath = requestPath;
		this.userId = userId;
		this.createdAt = createdAt;
	}

	public long getId() {
		return id;
	}

	public String getLevel() {
		return level;
	}

	public String getLoggerName() {
		return loggerName;
	}

	public String getMessage() {
		return message;
	}

	public String getStackTrace() {
		return stackTrace;
	}

	public String getRequestMethod() {
		return requestMethod;
	}

	public String getRequestPath() {
		return requestPath;
	}

	public UUID getUserId() {
		return userId;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public static void insert(Connection connection, String level, String loggerName, String message,
		String stackTrace, String requestMethod, String requestPath, UUID userId) {
		Preconditions.checkNotNull(level, "level");
		DSLContext dsl = DSL.using(connection);
		dsl.insertInto(Tables.API_ERROR_LOG.table)
			.set(Tables.API_ERROR_LOG.LEVEL, level)
			.set(Tables.API_ERROR_LOG.LOGGER_NAME, loggerName)
			.set(Tables.API_ERROR_LOG.MESSAGE, message)
			.set(Tables.API_ERROR_LOG.STACK_TRACE, stackTrace)
			.set(Tables.API_ERROR_LOG.REQUEST_METHOD, requestMethod)
			.set(Tables.API_ERROR_LOG.REQUEST_PATH, requestPath)
			.set(Tables.API_ERROR_LOG.USER_ID, userId)
			.set(Tables.API_ERROR_LOG.CREATED_AT, OffsetDateTime.now(ZoneOffset.UTC))
			.execute();
	}

	public static List<ApiErrorLogRow> listRecent(DSLContext dsl, int limit) {
		return dsl.selectFrom(Tables.API_ERROR_LOG.table)
			.orderBy(Tables.API_ERROR_LOG.CREATED_AT.desc())
			.limit(limit)
			.fetch(r -> new ApiErrorLogRow(
				r.get(Tables.API_ERROR_LOG.ID),
				r.get(Tables.API_ERROR_LOG.LEVEL),
				r.get(Tables.API_ERROR_LOG.LOGGER_NAME),
				r.get(Tables.API_ERROR_LOG.MESSAGE),
				r.get(Tables.API_ERROR_LOG.STACK_TRACE),
				r.get(Tables.API_ERROR_LOG.REQUEST_METHOD),
				r.get(Tables.API_ERROR_LOG.REQUEST_PATH),
				r.get(Tables.API_ERROR_LOG.USER_ID),
				r.get(Tables.API_ERROR_LOG.CREATED_AT).toInstant()
			));
	}
}
