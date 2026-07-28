package ca.refundr.highspring.common.error;

import ca.refundr.highspring.common.mail.MailSender;
import com.google.common.base.Preconditions;

/**
 * Emails the developer when the server hits an unexpected failure, including the full stack trace.
 */
public final class EmailErrorReporter implements ErrorReporter {

	private final MailSender mailSender;
	private final String developerEmail;

	public EmailErrorReporter(MailSender mailSender, String developerEmail) {
		this.mailSender = Preconditions.checkNotNull(mailSender, "mailSender");
		this.developerEmail = Preconditions.checkNotNull(developerEmail, "developerEmail");
	}

	@Override
	public void report(ErrorReport report) {
		Preconditions.checkNotNull(report, "report");
		String subject = "[Highspring] 500 on " + report.requestMethod() + " " + report.requestPath();
		String body = """
			A serious server error occurred.

			Message: %s
			Method: %s
			Path: %s
			User: %s

			Stack trace:
			%s
			""".formatted(
			report.message(),
			report.requestMethod(),
			report.requestPath(),
			report.userId() == null ? "(none)" : report.userId(),
			report.stackTraceAsString()
		);
		mailSender.send(developerEmail, subject, body);
	}
}
