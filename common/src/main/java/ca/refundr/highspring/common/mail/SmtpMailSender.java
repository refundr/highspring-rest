package ca.refundr.highspring.common.mail;

import com.google.common.base.Preconditions;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.util.Properties;

/**
 * Sends email through a normal SMTP server (for real developer alerts).
 */
public final class SmtpMailSender implements MailSender {

	private final Session session;
	private final String fromAddress;

	public SmtpMailSender(String host, int port, String username, String password, String fromAddress) {
		Preconditions.checkNotNull(host, "host");
		Preconditions.checkNotNull(fromAddress, "fromAddress");
		this.fromAddress = fromAddress;
		Properties props = new Properties();
		props.put("mail.smtp.host", host);
		props.put("mail.smtp.port", String.valueOf(port));
		props.put("mail.smtp.auth", username != null && !username.isBlank());
		props.put("mail.smtp.starttls.enable", "true");
		if (username != null && !username.isBlank()) {
			this.session = Session.getInstance(props, new jakarta.mail.Authenticator() {
				@Override
				protected jakarta.mail.PasswordAuthentication getPasswordAuthentication() {
					return new jakarta.mail.PasswordAuthentication(username, password);
				}
			});
		} else {
			this.session = Session.getInstance(props);
		}
	}

	@Override
	public void send(String to, String subject, String body) {
		Preconditions.checkNotNull(to, "to");
		Preconditions.checkNotNull(subject, "subject");
		Preconditions.checkNotNull(body, "body");
		try {
			MimeMessage message = new MimeMessage(session);
			message.setFrom(new InternetAddress(fromAddress));
			message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
			message.setSubject(subject);
			message.setText(body);
			Transport.send(message);
		} catch (Exception e) {
			throw new IllegalStateException("Failed to send email to " + to, e);
		}
	}
}
