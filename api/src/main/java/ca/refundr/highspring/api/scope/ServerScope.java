package ca.refundr.highspring.api.scope;

import ca.refundr.highspring.api.oauth.GoogleOAuthProvider;
import ca.refundr.highspring.api.oauth.OAuthProvider;
import ca.refundr.highspring.api.pricing.CartPricingService;
import ca.refundr.highspring.common.config.AppConfiguration;
import ca.refundr.highspring.common.error.CompositeErrorReporter;
import ca.refundr.highspring.common.error.EmailErrorReporter;
import ca.refundr.highspring.common.error.ErrorReporter;
import ca.refundr.highspring.common.error.LoggingErrorReporter;
import ca.refundr.highspring.common.error.SentryErrorReporter;
import ca.refundr.highspring.common.mail.LoggingMailSender;
import ca.refundr.highspring.common.mail.MailSender;
import ca.refundr.highspring.common.mail.SmtpMailSender;
import ca.refundr.highspring.database.error.DatabaseErrorReporter;
import ca.refundr.highspring.database.scope.DatabaseScope;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.base.Preconditions;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.client.HttpClient;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Set;

/**
 * Long-lived server services shared by every request.
 *
 * <p>Created once at process start ({@link ca.refundr.highspring.api.Server}). Holds:
 * <ul>
 *   <li>config + database pool</li>
 *   <li>OAuth provider (real Google, or a stub in tests)</li>
 *   <li>{@link CartPricingService}</li>
 *   <li>{@link ErrorReporter} composite (log + DB + email + Sentry stub)</li>
 * </ul>
 *
 * <p>Per-request state belongs in {@link RequestScope}, created via {@link #createRequest}.
 */
public final class ServerScope implements AutoCloseable {

	private final AppConfiguration configuration;
	private final DatabaseScope database;
	private final ObjectMapper objectMapper;
	private final OAuthProvider oAuthProvider;
	private final CartPricingService cartPricingService;
	private final ErrorReporter errorReporter;
	private final HttpClient httpClient;
	private final Set<String> adminEmails;
	private final Duration sessionLifetime;
	private final Path allureReportDir;
	private final Path javadocReportDir;
	private final boolean ownsHttpClient;

	public ServerScope(AppConfiguration configuration, DatabaseScope database) throws Exception {
		this(configuration, database, null);
	}

	public ServerScope(AppConfiguration configuration, DatabaseScope database, OAuthProvider oAuthProviderOverride)
		throws Exception {
		this.configuration = Preconditions.checkNotNull(configuration, "configuration");
		this.database = Preconditions.checkNotNull(database, "database");
		this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
		this.cartPricingService = new CartPricingService();
		this.adminEmails = configuration.getCsvSet("ADMIN_EMAILS");
		this.sessionLifetime = Duration.ofHours(configuration.getInt("SESSION_HOURS", 24));
		this.allureReportDir = Path.of(configuration.getString("ALLURE_REPORT_DIR", "published-allure"));
		this.javadocReportDir = Path.of(configuration.getString("JAVADOC_REPORT_DIR", "published-javadoc/apidocs"));

		if (oAuthProviderOverride != null) {
			this.httpClient = null;
			this.ownsHttpClient = false;
			this.oAuthProvider = oAuthProviderOverride;
		} else {
			this.httpClient = new HttpClient();
			this.httpClient.start();
			this.ownsHttpClient = true;
			this.oAuthProvider = new GoogleOAuthProvider(
				configuration.getString("GOOGLE_CLIENT_ID"),
				configuration.getString("GOOGLE_CLIENT_SECRET"),
				httpClient,
				objectMapper
			);
		}

		MailSender mailSender = createMailSender(configuration);
		this.errorReporter = new CompositeErrorReporter(List.of(
			new LoggingErrorReporter(),
			new DatabaseErrorReporter(database.getDataSource()),
			new EmailErrorReporter(mailSender, configuration.getString("DEVELOPER_ALERT_EMAIL")),
			new SentryErrorReporter()
		));
	}

	private static MailSender createMailSender(AppConfiguration configuration) {
		String mode = configuration.getString("MAIL_MODE", "logging");
		if ("smtp".equalsIgnoreCase(mode)) {
			return new SmtpMailSender(
				configuration.getString("SMTP_HOST"),
				configuration.getInt("SMTP_PORT", 587),
				configuration.getString("SMTP_USERNAME", ""),
				configuration.getString("SMTP_PASSWORD", ""),
				configuration.getString("SMTP_FROM", "highspring@localhost")
			);
		}
		return new LoggingMailSender();
	}

	public RequestScope createRequest(HttpServletRequest request, HttpServletResponse response) {
		return new RequestScope(this, request, response);
	}

	public AppConfiguration getConfiguration() {
		return configuration;
	}

	public DatabaseScope getDatabase() {
		return database;
	}

	public ObjectMapper getObjectMapper() {
		return objectMapper;
	}

	public OAuthProvider getOAuthProvider() {
		return oAuthProvider;
	}

	public CartPricingService getCartPricingService() {
		return cartPricingService;
	}

	public ErrorReporter getErrorReporter() {
		return errorReporter;
	}

	public Set<String> getAdminEmails() {
		return adminEmails;
	}

	public Duration getSessionLifetime() {
		return sessionLifetime;
	}

	public Path getAllureReportDir() {
		return allureReportDir;
	}

	/** Directory of generated JavaDoc HTML (served at /v1/admin/javadoc/). */
	public Path getJavadocReportDir() {
		return javadocReportDir;
	}

	@Override
	public void close() throws Exception {
		if (ownsHttpClient && httpClient != null) {
			httpClient.stop();
		}
	}
}
