package ca.refundr.highspring.database.row;

import ca.refundr.highspring.database.tables.Tables;
import com.google.common.base.Preconditions;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.impl.DSL;

import java.sql.Connection;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * Proof that someone is logged in — a temporary key the client sends with each request.
 */
public final class ApiSessionRow {

	private final UUID id;
	private final UUID userId;
	private final OffsetDateTime expiresAt;

	public ApiSessionRow(UUID id, UUID userId, OffsetDateTime expiresAt) {
		this.id = id;
		this.userId = userId;
		this.expiresAt = expiresAt;
	}

	public UUID getId() {
		return id;
	}

	public UUID getUserId() {
		return userId;
	}

	public OffsetDateTime getExpiresAt() {
		return expiresAt;
	}

	public static ApiSessionRow from(Record record) {
		return new ApiSessionRow(
			record.get(Tables.API_SESSION.ID),
			record.get(Tables.API_SESSION.USER_ID),
			record.get(Tables.API_SESSION.EXPIRES_AT)
		);
	}

	public static ApiSessionRow insert(Connection connection, UUID userId, Duration lifetime) {
		Preconditions.checkNotNull(userId, "userId");
		Preconditions.checkNotNull(lifetime, "lifetime");
		DSLContext dsl = DSL.using(connection);
		UUID id = UUID.randomUUID();
		OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
		OffsetDateTime expires = now.plus(lifetime);
		dsl.insertInto(Tables.API_SESSION.table)
			.set(Tables.API_SESSION.ID, id)
			.set(Tables.API_SESSION.USER_ID, userId)
			.set(Tables.API_SESSION.EXPIRES_AT, expires)
			.set(Tables.API_SESSION.LAST_ACTIVITY_AT, now)
			.set(Tables.API_SESSION.CREATED_AT, now)
			.execute();
		return new ApiSessionRow(id, userId, expires);
	}

	public static ApiSessionRow fetchActive(DSLContext dsl, UUID sessionId) {
		Record record = dsl.selectFrom(Tables.API_SESSION.table)
			.where(Tables.API_SESSION.ID.eq(sessionId))
			.and(Tables.API_SESSION.EXPIRES_AT.gt(OffsetDateTime.now(ZoneOffset.UTC)))
			.fetchOne();
		return record == null ? null : from(record);
	}

	public static void touch(Connection connection, UUID sessionId) {
		DSLContext dsl = DSL.using(connection);
		dsl.update(Tables.API_SESSION.table)
			.set(Tables.API_SESSION.LAST_ACTIVITY_AT, OffsetDateTime.now(ZoneOffset.UTC))
			.where(Tables.API_SESSION.ID.eq(sessionId))
			.execute();
	}

	/** Removes the session so the id can no longer authorize requests. */
	public static void delete(Connection connection, UUID sessionId) {
		Preconditions.checkNotNull(sessionId, "sessionId");
		DSLContext dsl = DSL.using(connection);
		dsl.deleteFrom(Tables.API_SESSION.table)
			.where(Tables.API_SESSION.ID.eq(sessionId))
			.execute();
	}
}
