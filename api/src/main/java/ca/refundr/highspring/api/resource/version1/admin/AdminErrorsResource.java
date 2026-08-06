package ca.refundr.highspring.api.resource.version1.admin;

import ca.refundr.highspring.api.resource.AbstractChildResource;
import ca.refundr.highspring.api.resource.AbstractResource;
import ca.refundr.highspring.api.scope.RequestScope;
import ca.refundr.highspring.api.util.ServerResponse;
import ca.refundr.highspring.database.row.ApiErrorLogRow;
import ca.refundr.highspring.domain.admin.ErrorLogResponse;
import org.eclipse.jetty.http.HttpMethod;
import org.jooq.impl.DSL;

import java.util.List;
import java.util.Map;

import static org.eclipse.jetty.http.HttpStatus.METHOD_NOT_ALLOWED_405;
import static org.eclipse.jetty.http.HttpStatus.OK_200;

/**
 * Lists and deletes serious server errors that were saved with their stack traces.
 */
public final class AdminErrorsResource extends AbstractChildResource<AdminResource> {

	public AdminErrorsResource(RequestScope scope, AdminResource parent) {
		super(scope, parent);
		supportedMethods.add(HttpMethod.GET.asString());
		supportedMethods.add(HttpMethod.DELETE.asString());
	}

	@Override
	public String getRelativePath() {
		return "errors/";
	}

	@Override
	public ServerResponse httpGet() {
		if (!scope.getRequest().acceptsJson()) {
			return writer -> writer.sendEmpty(METHOD_NOT_ALLOWED_405);
		}
		requireAdmin();
		List<ErrorLogResponse> errors = scope.getDatabase().transactionWithResult(connection -> {
			List<ApiErrorLogRow> rows = ApiErrorLogRow.listRecent(DSL.using(connection), 50);
			return rows.stream()
				.map(row -> new ErrorLogResponse(
					row.getId(),
					row.getLevel(),
					row.getLoggerName(),
					row.getMessage(),
					row.getStackTrace(),
					row.getRequestMethod(),
					row.getRequestPath(),
					row.getUserId(),
					row.getCreatedAt()
				))
				.toList();
		});
		return writer -> writer.sendJson(OK_200, errors);
	}

	@Override
	public ServerResponse httpDelete() {
		requireAdmin();
		int deleted = scope.getDatabase().transactionWithResult(ApiErrorLogRow::deleteAll);
		return writer -> writer.sendJson(OK_200, Map.of("deleted", deleted));
	}

	@Override
	protected <T extends AbstractResource> T getDescendantByPath(String relativePath) {
		return getDescendantFromChildByLong(relativePath, id -> new AdminErrorResource(scope, this, id));
	}
}
