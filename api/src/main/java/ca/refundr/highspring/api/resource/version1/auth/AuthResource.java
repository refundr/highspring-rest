package ca.refundr.highspring.api.resource.version1.auth;

import ca.refundr.highspring.api.resource.AbstractChildResource;
import ca.refundr.highspring.api.resource.AbstractResource;
import ca.refundr.highspring.api.resource.version1.Version1Resource;
import ca.refundr.highspring.api.resource.version1.auth.google.GoogleAuthCallbackResource;
import ca.refundr.highspring.api.resource.version1.auth.google.GoogleAuthUrlResource;
import ca.refundr.highspring.api.scope.RequestScope;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

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
 *   <li>{@link AuthLogoutResource} — {@code DELETE /v1/auth/logout/} deletes the current session</li>
 *   <li>{@link AuthE2eLoginResource} — {@code POST /v1/auth/e2e/login/} (only if {@code E2E_AUTH_ENABLED=true})</li>
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
		List<Supplier<? extends AbstractResource>> children = new ArrayList<>();
		children.add(() -> new GoogleAuthUrlResource(scope, this));
		children.add(() -> new GoogleAuthCallbackResource(scope, this));
		children.add(() -> new AuthLogoutResource(scope, this));
		if ("true".equalsIgnoreCase(scope.getConfiguration().getString("E2E_AUTH_ENABLED", "false"))) {
			children.add(() -> new AuthE2eLoginResource(scope, this));
		}
		return getDescendantFromChildren(relativePath, children);
	}
}
