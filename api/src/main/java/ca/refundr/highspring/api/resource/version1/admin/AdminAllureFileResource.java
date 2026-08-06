package ca.refundr.highspring.api.resource.version1.admin;

import ca.refundr.highspring.api.resource.AbstractChildResource;
import ca.refundr.highspring.api.resource.AbstractResource;
import ca.refundr.highspring.api.scope.RequestScope;
import ca.refundr.highspring.api.util.ServerResponse;
import org.eclipse.jetty.http.HttpMethod;

import java.io.IOException;

/**
 * Nested Allure asset under {@code /v1/admin/allure/...}.
 */
public final class AdminAllureFileResource extends AbstractChildResource<AdminAllureResource> {

	private final String relativeFile;

	public AdminAllureFileResource(RequestScope scope, AdminAllureResource parent, String relativeFile) {
		super(scope, parent);
		this.relativeFile = relativeFile;
		supportedMethods.add(HttpMethod.GET.asString());
	}

	@Override
	public String getRelativePath() {
		return relativeFile;
	}

	@Override
	public ServerResponse httpGet() throws IOException {
		requireAdmin();
		return parent.serveFile(relativeFile);
	}

	@Override
	protected <T extends AbstractResource> T getDescendantByPath(String relativePath) {
		return (T) new AdminAllureFileResource(scope, parent, relativeFile + relativePath);
	}
}
