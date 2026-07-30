package ca.refundr.highspring.database.scope;

import ca.refundr.highspring.common.config.AppConfiguration;
import ca.refundr.highspring.common.config.DatabaseUrls;
import com.google.common.base.Preconditions;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;

import javax.sql.DataSource;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Database connection used when the real server is running.
 * Applies schema migrations on startup so the tables stay up to date.
 */
public final class MainDatabaseScope implements DatabaseScope {

	private final AppConfiguration configuration;
	private final HikariDataSource dataSource;
	private final AtomicBoolean closed = new AtomicBoolean(false);

	public MainDatabaseScope(AppConfiguration configuration) {
		this.configuration = Preconditions.checkNotNull(configuration, "configuration");
		String rawUrl = configuration.getString("DATABASE_URL");
		HikariConfig hikari = new HikariConfig();
		hikari.setJdbcUrl(DatabaseUrls.toJdbcUrl(rawUrl));
		hikari.setUsername(DatabaseUrls.usernameFromUrl(rawUrl)
			.orElseGet(() -> configuration.getString("DATABASE_USERNAME")));
		hikari.setPassword(DatabaseUrls.passwordFromUrl(rawUrl)
			.orElseGet(() -> configuration.getString("DATABASE_PASSWORD")));
		hikari.setMaximumPoolSize(configuration.getInt("DATABASE_MAX_POOL", 10));
		hikari.setAutoCommit(false);
		hikari.setTransactionIsolation("TRANSACTION_READ_COMMITTED");
		hikari.setConnectionTimeout(Duration.ofSeconds(30).toMillis());
		this.dataSource = new HikariDataSource(hikari);

		Flyway.configure()
			.dataSource(dataSource)
			.locations("classpath:ca/refundr/highspring/database/migration")
			.load()
			.migrate();
	}

	@Override
	public DataSource getDataSource() {
		ensureOpen();
		return dataSource;
	}

	@Override
	public AppConfiguration getConfiguration() {
		return configuration;
	}

	private void ensureOpen() {
		Preconditions.checkState(!closed.get(), "Database scope is closed");
	}

	@Override
	public void close() {
		if (closed.compareAndSet(false, true)) {
			dataSource.close();
		}
	}
}
