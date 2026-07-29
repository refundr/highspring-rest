package ca.refundr.highspring.common.error;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.UUID;

/**
 * A snapshot of a serious server failure, ready to be saved and emailed.
 * Only used for unexpected "500" style problems — not for normal "bad request" answers.
 *
 * @param message        human-readable failure summary
 * @param throwable      exception that caused the failure; used to render the stack trace
 * @param requestMethod  HTTP method of the request that failed; may be {@code null}
 * @param requestPath    request path/URI at failure time; may be {@code null}
 * @param userId         authenticated user id if a session was present; otherwise {@code null}
 */
public record ErrorReport(
	String message,
	Throwable throwable,
	String requestMethod,
	String requestPath,
	UUID userId
) {
	/** Renders {@link #throwable} as a multi-line stack trace string for storage/email. */
	public String stackTraceAsString() {
		if (throwable == null) {
			return "";
		}
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		throwable.printStackTrace(pw);
		pw.flush();
		return sw.toString();
	}
}
