package ca.refundr.highspring.api.resource.version1.admin;

import ca.refundr.highspring.api.resource.AbstractChildResource;
import ca.refundr.highspring.api.resource.AbstractResource;
import ca.refundr.highspring.api.scope.RequestScope;
import ca.refundr.highspring.api.util.ServerResponse;
import ca.refundr.highspring.database.row.ApiErrorLogRow;
import org.eclipse.jetty.http.HttpMethod;

import static org.eclipse.jetty.http.HttpStatus.NOT_FOUND_404;
import static org.eclipse.jetty.http.HttpStatus.NO_CONTENT_204;

/**
 * A single saved error: {@code DELETE /v1/admin/errors/{id}/}.
 */
public final class AdminErrorResource extends AbstractChildResource<AdminErrorsResource> {

	private final long errorId;

	public AdminErrorResource(RequestScope scope, AdminErrorsResource parent, long errorId) {
		super(scope, parent);
		this.errorId = errorId;
		supportedMethods.add(HttpMethod.DELETE.asString());
	}

	@Override
	public String getRelativePath() {
		return errorId + "/";
	}

	@Override
	public ServerResponse httpDelete() {
		requireAdmin();
		boolean deleted = scope.getDatabase().transactionWithResult(connection ->
			ApiErrorLogRow.deleteById(connection, errorId)
		);
		if (!deleted) {
			return writer -> writer.sendText(NOT_FOUND_404, "Error log entry not found");
		}
		return writer -> writer.sendEmpty(NO_CONTENT_204);
	}

	@Override
	protected <T extends AbstractResource> T getDescendantByPath(String relativePath) {
		return null;
	}
}
