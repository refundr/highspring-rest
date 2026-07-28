package ca.refundr.highspring.database.scope;

import ca.refundr.highspring.common.config.AppConfiguration;
import com.google.common.base.Preconditions;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Function;

/**
 * Opens database work for the app. Callers borrow a connection, do their work,
 * and either save everything (commit) or undo it (rollback).
 */
public interface DatabaseScope extends AutoCloseable {

	DataSource getDataSource();

	AppConfiguration getConfiguration();

	default DSLContext dsl(Connection connection) {
		return DSL.using(connection, SQLDialect.POSTGRES);
	}

	default <T> T transactionWithResult(Function<Connection, T> work) {
		Preconditions.checkNotNull(work, "work");
		try (Connection connection = getDataSource().getConnection()) {
			boolean previous = connection.getAutoCommit();
			connection.setAutoCommit(false);
			try {
				T result = work.apply(connection);
				connection.commit();
				return result;
			} catch (RuntimeException e) {
				connection.rollback();
				throw e;
			} catch (Exception e) {
				connection.rollback();
				throw new IllegalStateException("Database transaction failed", e);
			} finally {
				connection.setAutoCommit(previous);
			}
		} catch (SQLException e) {
			throw new IllegalStateException("Unable to open database connection", e);
		}
	}

	default void transaction(java.util.function.Consumer<Connection> work) {
		transactionWithResult(connection -> {
			work.accept(connection);
			return null;
		});
	}

	@Override
	void close();
}
