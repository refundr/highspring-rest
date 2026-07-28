package ca.refundr.highspring.api.resource.version1.admin;

import ca.refundr.highspring.api.resource.AbstractChildResource;
import ca.refundr.highspring.api.resource.AbstractResource;
import ca.refundr.highspring.api.scope.RequestScope;
import ca.refundr.highspring.api.util.ServerResponse;
import org.eclipse.jetty.http.HttpMethod;

/**
 * Test-only endpoint that deliberately crashes so we can verify 500 handling.
 * Enabled only when ENABLE_BOOM_ENDPOINT=true.
 */
public final class AdminBoomResource extends AbstractChildResource<AdminResource> {

	public AdminBoomResource(RequestScope scope, AdminResource parent) {
		super(scope, parent);
		supportedMethods.add(HttpMethod.GET.asString());
	}

	@Override
	public String getRelativePath() {
		return "boom/";
	}

	@Override
	public ServerResponse httpGet() {
		requireAdmin();
		throw new IllegalStateException("Intentional boom for 500 pipeline testing");
	}

	@Override
	protected <T extends AbstractResource> T getDescendantByPath(String relativePath) {
		return null;
	}
}
