package ca.refundr.highspring.database.row;

import ca.refundr.highspring.database.model.UserRole;
import ca.refundr.highspring.database.tables.Tables;
import com.google.common.base.Preconditions;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.impl.DSL;

import java.math.BigDecimal;
import java.sql.Connection;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

/**
 * A signed-in person in the store (customer or admin).
 */
public final class AppUserRow {

	private final UUID id;
	private final String googleSub;
	private final String email;
	private final String displayName;
	private final UserRole role;

	public AppUserRow(UUID id, String googleSub, String email, String displayName, UserRole role) {
		this.id = id;
		this.googleSub = googleSub;
		this.email = email;
		this.displayName = displayName;
		this.role = role;
	}

	public UUID getId() {
		return id;
	}

	public String getGoogleSub() {
		return googleSub;
	}

	public String getEmail() {
		return email;
	}

	public String getDisplayName() {
		return displayName;
	}

	public UserRole getRole() {
		return role;
	}

	public static AppUserRow from(Record record) {
		return new AppUserRow(
			record.get(Tables.APP_USER.ID),
			record.get(Tables.APP_USER.GOOGLE_SUB),
			record.get(Tables.APP_USER.EMAIL),
			record.get(Tables.APP_USER.DISPLAY_NAME),
			UserRole.fromDb(record.get(Tables.APP_USER.ROLE))
		);
	}

	public static AppUserRow fetchById(DSLContext dsl, UUID id) {
		Record record = dsl.selectFrom(Tables.APP_USER.table)
			.where(Tables.APP_USER.ID.eq(id))
			.fetchOne();
		return record == null ? null : from(record);
	}

	public static AppUserRow fetchByGoogleSub(DSLContext dsl, String googleSub) {
		Record record = dsl.selectFrom(Tables.APP_USER.table)
			.where(Tables.APP_USER.GOOGLE_SUB.eq(googleSub))
			.fetchOne();
		return record == null ? null : from(record);
	}

	public static AppUserRow upsertFromGoogle(Connection connection, String googleSub, String email,
		String displayName, UserRole role) {
		Preconditions.checkNotNull(googleSub, "googleSub");
		Preconditions.checkNotNull(email, "email");
		Preconditions.checkNotNull(role, "role");
		DSLContext dsl = DSL.using(connection);
		AppUserRow existing = fetchByGoogleSub(dsl, googleSub);
		OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
		if (existing != null) {
			dsl.update(Tables.APP_USER.table)
				.set(Tables.APP_USER.EMAIL, email)
				.set(Tables.APP_USER.DISPLAY_NAME, displayName)
				.set(Tables.APP_USER.ROLE, DSL.field("?::user_role", String.class, role.name()))
				.set(Tables.APP_USER.UPDATED_AT, now)
				.where(Tables.APP_USER.ID.eq(existing.getId()))
				.execute();
			return fetchById(dsl, existing.getId());
		}
		UUID id = UUID.randomUUID();
		dsl.insertInto(Tables.APP_USER.table)
			.set(Tables.APP_USER.ID, id)
			.set(Tables.APP_USER.GOOGLE_SUB, googleSub)
			.set(Tables.APP_USER.EMAIL, email)
			.set(Tables.APP_USER.DISPLAY_NAME, displayName)
			.set(Tables.APP_USER.ROLE, DSL.field("?::user_role", String.class, role.name()))
			.set(Tables.APP_USER.CREATED_AT, now)
			.set(Tables.APP_USER.UPDATED_AT, now)
			.execute();
		return fetchById(dsl, id);
	}
}
