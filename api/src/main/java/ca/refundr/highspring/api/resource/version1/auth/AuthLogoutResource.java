package ca.refundr.highspring.api.resource.version1.auth;

import ca.refundr.highspring.api.resource.AbstractChildResource;
import ca.refundr.highspring.api.resource.AbstractResource;
import ca.refundr.highspring.api.scope.RequestScope;
import ca.refundr.highspring.api.util.ServerResponse;
import ca.refundr.highspring.database.row.ApiSessionRow;
import org.eclipse.jetty.http.HttpMethod;

import java.util.UUID;

import static org.eclipse.jetty.http.HttpStatus.NO_CONTENT_204;

/**
 * {@code DELETE /v1/auth/logout/} — deletes the caller's {@code api_session} row.
 *
 * <p>The Remix app calls this before clearing its cookie so a stolen session id stops working.
 */
public final class AuthLogoutResource extends AbstractChildResource<AuthResource> {

	public AuthLogoutResource(RequestScope scope, AuthResource parent) {
		super(scope, parent);
		supportedMethods.add(HttpMethod.DELETE.asString());
	}

	@Override
	public String getRelativePath() {
		return "logout/";
	}

	@Override
	public ServerResponse httpDelete() {
		requireSessionUser();
		UUID sessionId = scope.getCurrentSessionId();
		scope.getDatabase().transaction(connection -> ApiSessionRow.delete(connection, sessionId));
		return writer -> writer.sendEmpty(NO_CONTENT_204);
	}

	@Override
	protected <T extends AbstractResource> T getDescendantByPath(String relativePath) {
		return null;
	}
}
