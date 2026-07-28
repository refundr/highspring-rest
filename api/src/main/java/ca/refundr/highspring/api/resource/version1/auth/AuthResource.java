package ca.refundr.highspring.api.resource.version1.auth;

import ca.refundr.highspring.api.resource.AbstractChildResource;
import ca.refundr.highspring.api.resource.AbstractResource;
import ca.refundr.highspring.api.resource.version1.Version1Resource;
import ca.refundr.highspring.api.resource.version1.auth.google.GoogleAuthCallbackResource;
import ca.refundr.highspring.api.resource.version1.auth.google.GoogleAuthUrlResource;
import ca.refundr.highspring.api.scope.RequestScope;

import java.util.List;

/**
 * Login-related endpoints (/v1/auth/...).
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
