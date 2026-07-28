package ca.refundr.highspring.api.resource;

import ca.refundr.highspring.api.resource.version1.Version1Resource;
import ca.refundr.highspring.api.scope.RequestScope;
import ca.refundr.highspring.api.util.ServerResponse;

import java.util.List;

import static org.eclipse.jetty.http.HttpStatus.OK_200;

/**
 * Top of the URL tree. Routes /v1/... to the versioned API.
 */
public final class RootResource extends AbstractResource {

	public RootResource(RequestScope scope) {
		super(scope);
	}

	@Override
	public String getRelativePath() {
		return "";
	}

	@Override
	public ServerResponse httpGet() {
		return writer -> writer.sendJson(OK_200, java.util.Map.of(
			"name", "highspring",
			"message", "Shopping cart API"
		));
	}

	@Override
	protected <T extends AbstractResource> T getDescendantByPath(String relativePath) {
		return getDescendantFromChildren(relativePath, List.of(
			() -> new Version1Resource(scope, this)
		));
	}
}
