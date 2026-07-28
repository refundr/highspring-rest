package ca.refundr.highspring.common.error;

import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Placeholder for a future Sentry (or similar) integration.
 * Swap this in via CompositeErrorReporter when you are ready — no other code needs to change.
 */
public final class SentryErrorReporter implements ErrorReporter {

	private static final Logger log = LoggerFactory.getLogger(SentryErrorReporter.class);

	@Override
	public void report(ErrorReport report) {
		Preconditions.checkNotNull(report, "report");
		log.debug("SentryErrorReporter stub received failure: {} (not forwarded)", report.message());
	}
}
