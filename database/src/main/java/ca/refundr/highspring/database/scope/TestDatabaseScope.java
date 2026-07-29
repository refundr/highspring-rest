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
 * Isolated Postgres database for one automated test: create → migrate → use → drop.
 *
 * <h2>Why this is good database-test practice</h2>
 *
 * <ul>
 *   <li><b>Isolation</b> — Each instance gets its own database name
 *       ({@code {main}_test0}, {@code {main}_test1}, …). Tests never share rows, so
 *       order of execution does not matter and parallel runs do not collide.</li>
 *   <li><b>Real schema</b> — Flyway applies the same migration scripts as production
 *       ({@link MainDatabaseScope}). You are not testing against a hand-built mock schema
 *       that can drift from what ships.</li>
 *   <li><b>Clean teardown</b> — {@link #close()} drops the database. Leftover data cannot
 *       poison the next run or the developer’s main {@code DATABASE_URL} database.</li>
 *   <li><b>Same interface as production</b> — Implements {@link DatabaseScope}, so app code
 *       under test sees the same {@code transaction(…)} / {@code getDataSource()} API as
 *       the live server. Swapping test vs main is a constructor choice, not a fork of
 *       business logic.</li>
 *   <li><b>Conservative pool</b> — Small Hikari pool ({@link #MAX_CONNECTIONS_PER_TEST}),
 *       {@code autoCommit=false}, and {@code READ_COMMITTED} match how the app expects to
 *       use connections, without exhausting Postgres slots when many tests spin up.</li>
 *   <li><b>Admin connection separate from app pool</b> — {@code CREATE}/{@code DROP DATABASE}
 *       run against the {@code postgres} system database with a short-lived datasource.
 *       That is required (you cannot drop the DB you are connected to) and keeps DDL
 *       out of the application connection pool.</li>
 *   <li><b>Fast enough per test</b> — Creating an empty Postgres database and running a
 *       small Flyway set is typically a second or two locally. That is cheap compared with
 *       debugging flaky shared-DB tests, and still much lighter than starting Docker for
 *       every class. The tiny Hikari pool and dropping the whole DB (no per-table cleanup
 *       loops) keep setup/teardown simple and quick.</li>
 * </ul>
 *
 * <h2>Why this pattern is easy to reuse in other repos</h2>
 *
 * <ul>
 *   <li>It depends only on config keys ({@code DATABASE_URL}, username, password) and
 *       classpath Flyway locations — no Highspring domain types.</li>
 *   <li>The lifecycle is copy-friendly: parse JDBC URL → derive test name → create DB →
 *       migrate → expose {@link DataSource} → on close, drop DB. Other services can lift
 *       the class (or the idea) with a different migration package path.</li>
 *   <li>Pairing it with {@code AutoCloseable} / try-with-resources
 *       ({@code try (TestDatabaseScope db = new TestDatabaseScope(config))}) is a standard
 *       Java idiom every repo already understands.</li>
 *   <li>Prefer this over “truncate all tables between tests” when schemas grow: dropping
 *       the database resets extensions, sequences, and Flyway history in one shot.</li>
 * </ul>
 *
 * <p>Requires a local Postgres that allows {@code CREATE DATABASE} (typical for DBngin /
 * Docker CI). The main app database named in {@code DATABASE_URL} is never truncated;
 * only sibling {@code *_testN} databases are created and destroyed.
 *
 * @see DatabaseScope
 * @see MainDatabaseScope
 */
public final class TestDatabaseScope implements DatabaseScope {

	/** Keep pools tiny so many concurrent tests do not exhaust Postgres max_connections. */
	public static final int MAX_CONNECTIONS_PER_TEST = 2;

	/** Monotonic suffix so parallel or back-to-back tests get unique DB names. */
	private static final AtomicInteger NEXT_ID = new AtomicInteger();

	/** Postgres default maintenance DB — used only to CREATE/DROP the throwaway test DB. */
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

		// Derive sibling URLs from DATABASE_URL, e.g.
		//   jdbc:postgresql://localhost:5436/highspring
		// → system:  .../postgres
		// → test:    .../highspring_test0
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

		// Same migrations as MainDatabaseScope — schema under test === schema in prod.
		Flyway.configure()
			.dataSource(dataSource)
			.locations("classpath:ca/refundr/highspring/database/migration")
			.load()
			.migrate();
	}

	/** Create an empty database via the system catalog connection (not the app pool). */
	private void createDatabase() {
		HikariConfig systemConfig = new HikariConfig();
		baseConfig.copyStateTo(systemConfig);
		systemConfig.setJdbcUrl(systemJdbcUrl);
		try (HikariDataSource systemDatabase = new HikariDataSource(systemConfig);
		     Connection connection = systemDatabase.getConnection()) {
			// DDL for CREATE DATABASE cannot run inside a transaction.
			connection.setAutoCommit(true);
			try (Statement statement = connection.createStatement()) {
				statement.executeUpdate("DROP DATABASE IF EXISTS " + testDatabaseName);
				statement.executeUpdate("CREATE DATABASE " + testDatabaseName);
			}
		} catch (SQLException e) {
			throw new DataAccessException("Failed to create test database " + testDatabaseName, e);
		}
	}

	/** Remove the throwaway database after the test pool is closed. */
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

	/**
	 * Idempotent cleanup: close the pool, then drop the database.
	 * Safe to call more than once (e.g. try-with-resources + explicit close).
	 */
	@Override
	public void close() {
		if (closed.compareAndSet(false, true)) {
			dataSource.close();
			dropDatabase();
		}
	}
}
