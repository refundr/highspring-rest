package ca.refundr.highspring.api.resource.version1.admin;

import ca.refundr.highspring.api.resource.AbstractChildResource;
import ca.refundr.highspring.api.resource.AbstractResource;
import ca.refundr.highspring.api.scope.RequestScope;
import ca.refundr.highspring.api.util.ServerResponse;
import org.eclipse.jetty.http.HttpMethod;

import java.io.IOException;

/**
 * Nested Javadoc asset under {@code /v1/admin/javadoc/...}.
 */
public final class AdminJavadocFileResource extends AbstractChildResource<AdminJavadocResource> {

	private final String relativeFile;

	public AdminJavadocFileResource(RequestScope scope, AdminJavadocResource parent, String relativeFile) {
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
		return (T) new AdminJavadocFileResource(scope, parent, relativeFile + relativePath);
	}
}
