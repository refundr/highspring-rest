package ca.refundr.highspring.common.error;

import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Sends one failure report to several reporters at once (database, email, log, …).
 * If one reporter fails, the others still run.
 */
public final class CompositeErrorReporter implements ErrorReporter {

	private static final Logger log = LoggerFactory.getLogger(CompositeErrorReporter.class);
	private final List<ErrorReporter> reporters;

	public CompositeErrorReporter(List<ErrorReporter> reporters) {
		Preconditions.checkNotNull(reporters, "reporters");
		this.reporters = List.copyOf(reporters);
	}

	@Override
	public void report(ErrorReport report) {
		Preconditions.checkNotNull(report, "report");
		for (ErrorReporter reporter : reporters) {
			try {
				reporter.report(report);
			} catch (Exception e) {
				log.error("ErrorReporter {} failed while handling a 500", reporter.getClass().getSimpleName(), e);
			}
		}
	}
}
