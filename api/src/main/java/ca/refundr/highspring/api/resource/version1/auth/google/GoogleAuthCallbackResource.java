package ca.refundr.highspring.api.resource.version1.auth.google;

import ca.refundr.highspring.api.oauth.OAuthProvider;
import ca.refundr.highspring.api.resource.AbstractChildResource;
import ca.refundr.highspring.api.resource.AbstractResource;
import ca.refundr.highspring.api.resource.version1.auth.AuthResource;
import ca.refundr.highspring.api.scope.RequestScope;
import ca.refundr.highspring.api.util.ServerResponse;
import ca.refundr.highspring.api.util.exceptions.BadRequestException;
import ca.refundr.highspring.database.model.UserRole;
import ca.refundr.highspring.database.row.ApiSessionRow;
import ca.refundr.highspring.database.row.AppUserRow;
import ca.refundr.highspring.domain.auth.GoogleAuthCallbackRequest;
import ca.refundr.highspring.domain.auth.SessionResponse;
import org.eclipse.jetty.http.HttpMethod;

import java.io.IOException;

import static org.eclipse.jetty.http.HttpStatus.BAD_REQUEST_400;
import static org.eclipse.jetty.http.HttpStatus.METHOD_NOT_ALLOWED_405;
import static org.eclipse.jetty.http.HttpStatus.OK_200;
import static org.eclipse.jetty.http.HttpStatus.UNAUTHORIZED_401;

/**
 * Finishes Google login: trades the code for a profile, creates/updates the user, and starts a session.
 */
public final class GoogleAuthCallbackResource extends AbstractChildResource<AuthResource> {

	public GoogleAuthCallbackResource(RequestScope scope, AuthResource parent) {
		super(scope, parent);
		supportedMethods.add(HttpMethod.POST.asString());
	}

	@Override
	public String getRelativePath() {
		return "google/callback/";
	}

	@Override
	public ServerResponse httpPost() throws IOException {
		if (!scope.getRequest().acceptsJson()) {
			return writer -> writer.sendEmpty(METHOD_NOT_ALLOWED_405);
		}
		GoogleAuthCallbackRequest body = scope.getRequest().getBody(GoogleAuthCallbackRequest.class);
		if (body == null || body.code() == null || body.code().isBlank()
			|| body.redirectUri() == null || body.redirectUri().isBlank()) {
			return writer -> writer.sendText(BAD_REQUEST_400, "code and redirectUri are required");
		}

		OAuthProvider.GoogleProfile profile;
		try {
			profile = scope.getOAuthProvider().exchangeCode(body.code(), body.redirectUri());
		} catch (BadRequestException e) {
			return writer -> writer.sendText(UNAUTHORIZED_401, "Authorization is required.");
		}

		// Demo default: every new Google sign-in is ADMIN so the admin UI is easy to show.
		UserRole role = UserRole.ADMIN;

		SessionResponse sessionResponse = scope.getDatabase().transactionWithResult(connection -> {
			AppUserRow user = AppUserRow.upsertFromGoogle(
				connection,
				profile.subject(),
				profile.email(),
				profile.displayName(),
				role
			);
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
