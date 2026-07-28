package ca.refundr.highspring.api.resource.version1.auth.google;

import ca.refundr.highspring.api.resource.AbstractChildResource;
import ca.refundr.highspring.api.resource.AbstractResource;
import ca.refundr.highspring.api.resource.version1.auth.AuthResource;
import ca.refundr.highspring.api.scope.RequestScope;
import ca.refundr.highspring.api.util.ServerResponse;
import ca.refundr.highspring.domain.auth.AuthUrlResponse;
import ca.refundr.highspring.domain.auth.GoogleAuthUrlRequest;
import org.eclipse.jetty.http.HttpMethod;

import java.io.IOException;

import static org.eclipse.jetty.http.HttpStatus.BAD_REQUEST_400;
import static org.eclipse.jetty.http.HttpStatus.METHOD_NOT_ALLOWED_405;
import static org.eclipse.jetty.http.HttpStatus.OK_200;

/**
 * Builds the Google sign-in link the browser should open.
 */
public final class GoogleAuthUrlResource extends AbstractChildResource<AuthResource> {

	public GoogleAuthUrlResource(RequestScope scope, AuthResource parent) {
		super(scope, parent);
		supportedMethods.add(HttpMethod.POST.asString());
	}

	@Override
	public String getRelativePath() {
		return "google/url/";
	}

	@Override
	public ServerResponse httpPost() throws IOException {
		if (!scope.getRequest().acceptsJson()) {
			return writer -> writer.sendEmpty(METHOD_NOT_ALLOWED_405);
		}
		GoogleAuthUrlRequest body = scope.getRequest().getBody(GoogleAuthUrlRequest.class);
		if (body == null || body.redirectUri() == null || body.redirectUri().isBlank()) {
			return writer -> writer.sendText(BAD_REQUEST_400, "redirectUri is required");
		}
		String uri = scope.getOAuthProvider().buildAuthorizationUrl(
			body.redirectUri(),
			body.state() == null ? "" : body.state()
		);
		return writer -> writer.sendJson(OK_200, new AuthUrlResponse(uri));
	}

	@Override
	protected <T extends AbstractResource> T getDescendantByPath(String relativePath) {
		return null;
	}
}
