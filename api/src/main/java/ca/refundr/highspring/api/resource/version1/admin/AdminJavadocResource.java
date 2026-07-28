package ca.refundr.highspring.api.resource.version1.admin;

import ca.refundr.highspring.api.resource.AbstractChildResource;
import ca.refundr.highspring.api.resource.AbstractResource;
import ca.refundr.highspring.api.scope.RequestScope;
import ca.refundr.highspring.api.util.ServerResponse;
import org.eclipse.jetty.http.HttpMethod;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.eclipse.jetty.http.HttpStatus.NOT_FOUND_404;
import static org.eclipse.jetty.http.HttpStatus.OK_200;

/**
 * Serves published JavaDoc (and the HTTP status guide) to admins only.
 *
 * <p>Nested paths under {@code /v1/admin/javadoc/} map to files in {@code JAVADOC_REPORT_DIR}
 * (default {@code published-javadoc/apidocs/}). Generate with:
 * {@code mvn javadoc:aggregate} or {@code mvn verify}.
 *
 * <p>Start at {@code index.html} — overview links to {@link ca.refundr.highspring.api.HttpStatusGuide}
 * for REST error codes.
 */
public final class AdminJavadocResource extends AbstractChildResource<AdminResource> {

	public AdminJavadocResource(RequestScope scope, AdminResource parent) {
		super(scope, parent);
		supportedMethods.add(HttpMethod.GET.asString());
	}

	@Override
	public String getRelativePath() {
		return "javadoc/";
	}

	@Override
	public ServerResponse httpGet() throws IOException {
		requireAdmin();
		return serveFile("");
	}

	@Override
	protected <T extends AbstractResource> T getDescendantByPath(String relativePath) {
		return (T) new AdminJavadocFileResource(scope, this, relativePath);
	}

	ServerResponse serveFile(String relativeFile) throws IOException {
		Path root = scope.getJavadocReportDir().toAbsolutePath().normalize();
		String cleaned = relativeFile == null ? "" : relativeFile;
		if (cleaned.startsWith("/")) {
			cleaned = cleaned.substring(1);
		}
		if (cleaned.isBlank() || cleaned.endsWith("/")) {
			cleaned = cleaned + "index.html";
		}
		Path target = root.resolve(cleaned).normalize();
		if (!target.startsWith(root) || !Files.isRegularFile(target)) {
			return writer -> writer.sendText(NOT_FOUND_404,
				"Javadoc not found. Run: mvn javadoc:aggregate   (or mvn verify)");
		}
		String contentType = AdminAllureResource.contentTypeFor(target.getFileName().toString());
		byte[] bytes = Files.readAllBytes(target);
		return writer -> writer.sendBytes(OK_200, contentType, bytes);
	}

	/**
	 * Nested Javadoc asset under /v1/admin/javadoc/...
	 */
	public static final class AdminJavadocFileResource extends AbstractChildResource<AdminJavadocResource> {

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
}
