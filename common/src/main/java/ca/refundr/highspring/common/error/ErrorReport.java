package ca.refundr.highspring.common.error;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.UUID;

/**
 * A snapshot of a serious server failure, ready to be saved and emailed.
 * Only used for unexpected "500" style problems — not for normal "bad request" answers.
 */
public record ErrorReport(
	String message,
	Throwable throwable,
	String requestMethod,
	String requestPath,
	UUID userId
) {
	public String stackTraceAsString() {
		if (throwable == null) {
			return "";
		}
		StringWriter sw = new StringWriter();
		throwable.printStackTrace(new PrintWriter(sw));
		return sw.toString();
	}
}
