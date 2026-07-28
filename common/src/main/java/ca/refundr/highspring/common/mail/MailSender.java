package ca.refundr.highspring.common.mail;

/**
 * Sends plain-text email. Local setups can use a console implementation;
 * production can plug in SMTP without changing callers.
 */
public interface MailSender {
	void send(String to, String subject, String body);
}
