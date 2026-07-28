package ca.refundr.highspring.database.scope;

import ca.refundr.highspring.common.config.AppConfiguration;
import com.google.common.base.Preconditions;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;
import org.jooq.exception.DataAccessException;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Creates a fresh throwaway Postgres database for one test, migrates it, then deletes it when done.
 * This keeps tests from stepping on each other's data.
 */
public final class TestDatabaseScope implements DatabaseScope {

	public static final int MAX_CONNECTIONS_PER_TEST = 2;
	private static final AtomicInteger NEXT_ID = new AtomicInteger();
	private static final String SYSTEM_DATABASE_NAME = "postgres";

	private final AppConfiguration configuration;
	private final HikariDataSource dataSource;
	private final String testDatabaseName;
	private final String systemJdbcUrl;
	private final HikariConfig baseConfig;
	private final AtomicBoolean closed = new AtomicBoolean(false);

	public TestDatabaseScope(AppConfiguration configuration) {
		this.configuration = Preconditions.checkNotNull(configuration, "configuration");
		int id = NEXT_ID.getAndIncrement();

		String mainJdbcUrl = configuration.getString("DATABASE_URL");
		int afterName = mainJdbcUrl.lastIndexOf('?');
		if (afterName == -1) {
			afterName = mainJdbcUrl.length();
		}
		int beforeName = mainJdbcUrl.lastIndexOf('/', afterName - 1);
		Preconditions.checkArgument(beforeName >= 0, "Invalid DATABASE_URL: %s", mainJdbcUrl);

		String mainDatabaseName = mainJdbcUrl.substring(beforeName + 1, afterName);
		this.testDatabaseName = mainDatabaseName + "_test" + id;
		this.systemJdbcUrl = mainJdbcUrl.substring(0, beforeName + 1) + SYSTEM_DATABASE_NAME
			+ mainJdbcUrl.substring(afterName);
		String testJdbcUrl = mainJdbcUrl.substring(0, beforeName + 1) + testDatabaseName
			+ mainJdbcUrl.substring(afterName);

		this.baseConfig = new HikariConfig();
		baseConfig.setAutoCommit(false);
		baseConfig.setMaximumPoolSize(MAX_CONNECTIONS_PER_TEST);
		baseConfig.setTransactionIsolation("TRANSACTION_READ_COMMITTED");
		baseConfig.setConnectionTimeout(Duration.ofSeconds(30).toMillis());
		baseConfig.setUsername(configuration.getString("DATABASE_USERNAME"));
		baseConfig.setPassword(configuration.getString("DATABASE_PASSWORD"));

		createDatabase();

		HikariConfig testConfig = new HikariConfig();
		baseConfig.copyStateTo(testConfig);
		testConfig.setJdbcUrl(testJdbcUrl);
		this.dataSource = new HikariDataSource(testConfig);

		Flyway.configure()
			.dataSource(dataSource)
			.locations("classpath:ca/refundr/highspring/database/migration")
			.load()
			.migrate();
	}

	private void createDatabase() {
		HikariConfig systemConfig = new HikariConfig();
		baseConfig.copyStateTo(systemConfig);
		systemConfig.setJdbcUrl(systemJdbcUrl);
		try (HikariDataSource systemDatabase = new HikariDataSource(systemConfig);
		     Connection connection = systemDatabase.getConnection()) {
			connection.setAutoCommit(true);
			try (Statement statement = connection.createStatement()) {
				statement.executeUpdate("DROP DATABASE IF EXISTS " + testDatabaseName);
				statement.executeUpdate("CREATE DATABASE " + testDatabaseName);
			}
		} catch (SQLException e) {
			throw new DataAccessException("Failed to create test database " + testDatabaseName, e);
		}
	}

	private void dropDatabase() {
		HikariConfig systemConfig = new HikariConfig();
		baseConfig.copyStateTo(systemConfig);
		systemConfig.setJdbcUrl(systemJdbcUrl);
		try (HikariDataSource systemDatabase = new HikariDataSource(systemConfig);
		     Connection connection = systemDatabase.getConnection()) {
			connection.setAutoCommit(true);
			try (Statement statement = connection.createStatement()) {
				statement.executeUpdate("DROP DATABASE IF EXISTS " + testDatabaseName);
			}
		} catch (SQLException e) {
			throw new DataAccessException("Failed to drop test database " + testDatabaseName, e);
		}
	}

	@Override
	public DataSource getDataSource() {
		Preconditions.checkState(!closed.get(), "Database scope is closed");
		return dataSource;
	}

	@Override
	public AppConfiguration getConfiguration() {
		return configuration;
	}

	public String getTestDatabaseName() {
		return testDatabaseName;
	}

	@Override
	public void close() {
		if (closed.compareAndSet(false, true)) {
			dataSource.close();
			dropDatabase();
		}
	}
}
