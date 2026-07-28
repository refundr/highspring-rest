package ca.refundr.highspring.database.error;

import ca.refundr.highspring.common.error.ErrorReport;
import ca.refundr.highspring.common.error.ErrorReporter;
import ca.refundr.highspring.database.row.ApiErrorLogRow;
import com.google.common.base.Preconditions;

import javax.sql.DataSource;
import java.sql.Connection;

/**
 * Saves a serious server failure (including the stack trace) into the database for later review.
 */
public final class DatabaseErrorReporter implements ErrorReporter {

	private final DataSource dataSource;

	public DatabaseErrorReporter(DataSource dataSource) {
		this.dataSource = Preconditions.checkNotNull(dataSource, "dataSource");
	}

	@Override
	public void report(ErrorReport report) {
		Preconditions.checkNotNull(report, "report");
		try (Connection connection = dataSource.getConnection()) {
			boolean previous = connection.getAutoCommit();
			connection.setAutoCommit(true);
			try {
				ApiErrorLogRow.insert(
					connection,
					"ERROR",
					"highspring.api",
					report.message(),
					report.stackTraceAsString(),
					report.requestMethod(),
					report.requestPath(),
					report.userId()
				);
			} finally {
				connection.setAutoCommit(previous);
			}
		} catch (Exception e) {
			throw new IllegalStateException("Failed to persist api_error_log", e);
		}
	}
}
