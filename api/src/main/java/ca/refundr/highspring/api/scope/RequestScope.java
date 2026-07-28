package ca.refundr.highspring.api.scope;

import ca.refundr.highspring.api.oauth.OAuthProvider;
import ca.refundr.highspring.api.pricing.CartPricingService;
import ca.refundr.highspring.api.rest.RestRequest;
import ca.refundr.highspring.api.util.RestResponseWriter;
import ca.refundr.highspring.common.config.AppConfiguration;
import ca.refundr.highspring.common.error.ErrorReporter;
import ca.refundr.highspring.database.row.AppUserRow;
import ca.refundr.highspring.database.scope.DatabaseScope;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;

/**
 * Everything one HTTP request needs: database, config, auth helpers, and response writer.
 */
public final class RequestScope implements AutoCloseable {

	private final ServerScope serverScope;
	private final RestRequest request;
	private final RestResponseWriter responseWriter;
	private AppUserRow currentUser;
	private UUID currentSessionId;

	public RequestScope(ServerScope serverScope, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
		this.serverScope = Preconditions.checkNotNull(serverScope, "serverScope");
		this.request = new RestRequest(httpRequest, serverScope.getObjectMapper());
		this.responseWriter = new RestResponseWriter(httpResponse, serverScope.getObjectMapper());
	}

	public RestRequest getRequest() {
		return request;
	}

	public RestResponseWriter getResponseWriter() {
		return responseWriter;
	}

	public DatabaseScope getDatabase() {
		return serverScope.getDatabase();
	}

	public AppConfiguration getConfiguration() {
		return serverScope.getConfiguration();
	}

	public ObjectMapper getObjectMapper() {
		return serverScope.getObjectMapper();
	}

	public OAuthProvider getOAuthProvider() {
		return serverScope.getOAuthProvider();
	}

	public CartPricingService getCartPricingService() {
		return serverScope.getCartPricingService();
	}

	public ErrorReporter getErrorReporter() {
		return serverScope.getErrorReporter();
	}

	public Set<String> getAdminEmails() {
		return serverScope.getAdminEmails();
	}

	public Duration getSessionLifetime() {
		return serverScope.getSessionLifetime();
	}

	public Path getAllureReportDir() {
		return serverScope.getAllureReportDir();
	}

	public AppUserRow getCurrentUser() {
		return currentUser;
	}

	public void setCurrentUser(AppUserRow currentUser) {
		this.currentUser = currentUser;
	}

	public UUID getCurrentSessionId() {
		return currentSessionId;
	}

	public void setCurrentSessionId(UUID currentSessionId) {
		this.currentSessionId = currentSessionId;
	}

	@Override
	public void close() {
		// request-scoped; nothing to release yet
	}
}
