package ca.refundr.highspring.common.error;

import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Always writes the failure to the application log.
 */
public final class LoggingErrorReporter implements ErrorReporter {

	private static final Logger log = LoggerFactory.getLogger(LoggingErrorReporter.class);

	@Override
	public void report(ErrorReport report) {
		Preconditions.checkNotNull(report, "report");
		log.error("Unhandled server error on {} {}: {}",
			report.requestMethod(), report.requestPath(), report.message(), report.throwable());
	}
}
