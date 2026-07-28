package ca.refundr.highspring.api.resource.version1.me;

import ca.refundr.highspring.api.resource.AbstractChildResource;
import ca.refundr.highspring.api.resource.AbstractResource;
import ca.refundr.highspring.api.resource.version1.Version1Resource;
import ca.refundr.highspring.api.scope.RequestScope;
import ca.refundr.highspring.api.util.ServerResponse;
import ca.refundr.highspring.database.row.AppUserRow;
import ca.refundr.highspring.domain.auth.SessionResponse;
import org.eclipse.jetty.http.HttpMethod;

import static org.eclipse.jetty.http.HttpStatus.METHOD_NOT_ALLOWED_405;
import static org.eclipse.jetty.http.HttpStatus.OK_200;

/**
 * {@code GET /v1/me/} — returns the signed-in user (session id, email, role).
 *
 * <p>The Remix shop loader calls this after login so the UI can refresh role (e.g. after
 * {@code ADMIN_EMAILS} changes) without forcing another Google sign-in.
 */
public final class MeResource extends AbstractChildResource<Version1Resource> {

	public MeResource(RequestScope scope, Version1Resource parent) {
		super(scope, parent);
		supportedMethods.add(HttpMethod.GET.asString());
	}

	@Override
	public String getRelativePath() {
		return "me/";
	}

	@Override
	public ServerResponse httpGet() {
		if (!scope.getRequest().acceptsJson()) {
			return writer -> writer.sendEmpty(METHOD_NOT_ALLOWED_405);
		}
		AppUserRow user = requireSessionUser();
		SessionResponse response = new SessionResponse(
			scope.getCurrentSessionId(),
			user.getId(),
			user.getEmail(),
			user.getDisplayName(),
			user.getRole().name()
		);
		return writer -> writer.sendJson(OK_200, response);
	}

	@Override
	protected <T extends AbstractResource> T getDescendantByPath(String relativePath) {
		return null;
	}
}
