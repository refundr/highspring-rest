package ca.refundr.highspring.api.resource.version1.auth;

import ca.refundr.highspring.api.resource.AbstractChildResource;
import ca.refundr.highspring.api.resource.AbstractResource;
import ca.refundr.highspring.api.scope.RequestScope;
import ca.refundr.highspring.api.util.ServerResponse;
import ca.refundr.highspring.database.model.UserRole;
import ca.refundr.highspring.database.row.ApiSessionRow;
import ca.refundr.highspring.database.row.AppUserRow;
import ca.refundr.highspring.domain.auth.E2eLoginRequest;
import ca.refundr.highspring.domain.auth.SessionResponse;
import org.eclipse.jetty.http.HttpMethod;

import java.io.IOException;

import static org.eclipse.jetty.http.HttpStatus.BAD_REQUEST_400;
import static org.eclipse.jetty.http.HttpStatus.METHOD_NOT_ALLOWED_405;
import static org.eclipse.jetty.http.HttpStatus.NOT_FOUND_404;
import static org.eclipse.jetty.http.HttpStatus.OK_200;

/**
 *
 * It’s a test-only fake login that skips Google.
 *
 * POST /v1/auth/e2e/login/ with { email, displayName }:
 *
 * Only works if E2E_AUTH_ENABLED=true (otherwise 404).
 * Upserts an app_user with a fake Google id like e2e:you@example.com (never talks to Google).
 * Inserts a real api_session row.
 * Returns the same kind of session JSON as the real Google callback (sessionId, email, role, …). Role is hardcoded ADMIN.
 * Playwright uses that so checkout smoke tests don’t need a Google popup. Remix then hits /e2e/session with a secret to put that session into the cookie.
 *
 * Not for production. Real users still go through Google OAuth.
 *
 * {@code POST /v1/auth/e2e/login/} — create a real {@code api_session} without Google.
 *
 * <p>Registered only when {@code E2E_AUTH_ENABLED=true}. Used by Playwright smoke tests so
 * checkout can run without a browser Google OAuth dance. Never enable in production.
 */
public final class AuthE2eLoginResource extends AbstractChildResource<AuthResource> {

	public AuthE2eLoginResource(RequestScope scope, AuthResource parent) {
		super(scope, parent);
		supportedMethods.add(HttpMethod.POST.asString());
	}

	@Override
	public String getRelativePath() {
		return "e2e/login/";
	}

	@Override
	public ServerResponse httpPost() throws IOException {
		if (!"true".equalsIgnoreCase(scope.getConfiguration().getString("E2E_AUTH_ENABLED", "false"))) {
			return writer -> writer.sendText(NOT_FOUND_404, "Not found");
		}
		if (!scope.getRequest().acceptsJson()) {
			return writer -> writer.sendEmpty(METHOD_NOT_ALLOWED_405);
		}
		E2eLoginRequest body = scope.getRequest().getBody(E2eLoginRequest.class);
		if (body == null || body.email() == null || body.email().isBlank()) {
			return writer -> writer.sendText(BAD_REQUEST_400, "email is required");
		}

		String email = body.email().trim().toLowerCase();
		String displayName = body.displayName() == null || body.displayName().isBlank()
			? "E2E Shopper"
			: body.displayName().trim();
		String googleSub = "e2e:" + email;
		UserRole role = UserRole.ADMIN;

		SessionResponse sessionResponse = scope.getDatabase().transactionWithResult(connection -> {
			AppUserRow user = AppUserRow.upsertFromGoogle(connection, googleSub, email, displayName, role);
			ApiSessionRow session = ApiSessionRow.insert(connection, user.getId(), scope.getSessionLifetime());
			return new SessionResponse(
				session.getId(),
				user.getId(),
				user.getEmail(),
				user.getDisplayName(),
				user.getRole().name()
			);
		});

		return writer -> writer.sendJson(OK_200, sessionResponse);
	}

	@Override
	protected <T extends AbstractResource> T getDescendantByPath(String relativePath) {
		return null;
	}
}
