package ca.refundr.highspring.api.resource.version1.auth;

import ca.refundr.highspring.api.resource.AbstractChildResource;
import ca.refundr.highspring.api.resource.AbstractResource;
import ca.refundr.highspring.api.resource.version1.Version1Resource;
import ca.refundr.highspring.api.resource.version1.auth.google.GoogleAuthCallbackResource;
import ca.refundr.highspring.api.resource.version1.auth.google.GoogleAuthUrlResource;
import ca.refundr.highspring.api.scope.RequestScope;

import java.util.List;

/**
 * Login-related endpoints under {@code /v1/auth/}.
 *
 * <p>This class is only a folder in the resource tree. Real work lives in:
 * <ul>
 *   <li>{@link ca.refundr.highspring.api.resource.version1.auth.google.GoogleAuthUrlResource} —
 *       {@code POST /v1/auth/google/url/} builds the Google consent URL</li>
 *   <li>{@link ca.refundr.highspring.api.resource.version1.auth.google.GoogleAuthCallbackResource} —
 *       {@code POST /v1/auth/google/callback/} exchanges the code for a profile and creates an
 *       {@code api_session}</li>
 * </ul>
 *
 * <p>The Remix app calls these from the browser (via its own loaders); the API never redirects the
 * browser to Google directly for the callback exchange.
 */
public final class AuthResource extends AbstractChildResource<Version1Resource> {

	public AuthResource(RequestScope scope, Version1Resource parent) {
		super(scope, parent);
	}

	@Override
	public String getRelativePath() {
		return "auth/";
	}

	@Override
	protected <T extends AbstractResource> T getDescendantByPath(String relativePath) {
		return getDescendantFromChildren(relativePath, List.of(
			() -> new GoogleAuthUrlResource(scope, this),
			() -> new GoogleAuthCallbackResource(scope, this)
		));
	}
}
