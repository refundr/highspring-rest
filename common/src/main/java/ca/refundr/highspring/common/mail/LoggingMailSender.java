package ca.refundr.highspring.common.mail;

import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Prints emails to the log instead of sending them — handy for local development and tests.
 */
public final class LoggingMailSender implements MailSender {

	private static final Logger log = LoggerFactory.getLogger(LoggingMailSender.class);

	@Override
	public void send(String to, String subject, String body) {
		Preconditions.checkNotNull(to, "to");
		Preconditions.checkNotNull(subject, "subject");
		Preconditions.checkNotNull(body, "body");
		log.info("MAIL to={} subject={}\n{}", to, subject, body);
	}
}
