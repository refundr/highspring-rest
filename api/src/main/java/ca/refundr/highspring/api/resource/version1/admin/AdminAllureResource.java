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
 * Serves the published Allure test report to admins only.
 * Nested paths under /v1/admin/allure/ map to files in ALLURE_REPORT_DIR.
 */
public final class AdminAllureResource extends AbstractChildResource<AdminResource> {

	public AdminAllureResource(RequestScope scope, AdminResource parent) {
		super(scope, parent);
		supportedMethods.add(HttpMethod.GET.asString());
	}

	@Override
	public String getRelativePath() {
		return "allure/";
	}

	@Override
	public ServerResponse httpGet() throws IOException {
		requireAdmin();
		return serveFile("");
	}

	@Override
	protected <T extends AbstractResource> T getDescendantByPath(String relativePath) {
		return (T) new AdminAllureFileResource(scope, this, relativePath);
	}

	ServerResponse serveFile(String relativeFile) throws IOException {
		Path root = scope.getAllureReportDir().toAbsolutePath().normalize();
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
				"Allure report not found. Run tests and publish the report first.");
		}
		String contentType = contentTypeFor(target.getFileName().toString());
		byte[] bytes = Files.readAllBytes(target);
		return writer -> writer.sendBytes(OK_200, contentType, bytes);
	}

	/**
	 * Nested Allure asset under /v1/admin/allure/...
	 */
	public static final class AdminAllureFileResource extends AbstractChildResource<AdminAllureResource> {

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

	static String contentTypeFor(String fileName) {
		String lower = fileName.toLowerCase();
		if (lower.endsWith(".html")) return "text/html; charset=utf-8";
		if (lower.endsWith(".js")) return "application/javascript; charset=utf-8";
		if (lower.endsWith(".css")) return "text/css; charset=utf-8";
		if (lower.endsWith(".json")) return "application/json; charset=utf-8";
		if (lower.endsWith(".svg")) return "image/svg+xml";
		if (lower.endsWith(".png")) return "image/png";
		if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
		return "application/octet-stream";
	}
}
