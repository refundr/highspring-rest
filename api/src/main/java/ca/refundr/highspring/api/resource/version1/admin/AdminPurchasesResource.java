package ca.refundr.highspring.api.resource.version1.admin;

import ca.refundr.highspring.api.resource.AbstractChildResource;
import ca.refundr.highspring.api.resource.AbstractResource;
import ca.refundr.highspring.api.resource.version1.purchase.PurchasesResource;
import ca.refundr.highspring.api.scope.RequestScope;
import ca.refundr.highspring.api.util.ServerResponse;
import ca.refundr.highspring.database.row.PurchaseRow;
import ca.refundr.highspring.domain.purchase.PurchaseResponse;
import org.eclipse.jetty.http.HttpMethod;
import org.jooq.impl.DSL;

import java.util.List;

import static org.eclipse.jetty.http.HttpStatus.METHOD_NOT_ALLOWED_405;
import static org.eclipse.jetty.http.HttpStatus.OK_200;

/**
 * Recent purchases for admins.
 */
public final class AdminPurchasesResource extends AbstractChildResource<AdminResource> {

	public AdminPurchasesResource(RequestScope scope, AdminResource parent) {
		super(scope, parent);
		supportedMethods.add(HttpMethod.GET.asString());
	}

	@Override
	public String getRelativePath() {
		return "purchases/";
	}

	@Override
	public ServerResponse httpGet() {
		if (!scope.getRequest().acceptsJson()) {
			return writer -> writer.sendEmpty(METHOD_NOT_ALLOWED_405);
		}
		requireAdmin();
		List<PurchaseResponse> purchases = scope.getDatabase().transactionWithResult(connection -> {
			List<PurchaseRow> rows = PurchaseRow.listRecent(DSL.using(connection), 50);
			return rows.stream().map(PurchasesResource::toResponse).toList();
		});
		return writer -> writer.sendJson(OK_200, purchases);
	}

	@Override
	protected <T extends AbstractResource> T getDescendantByPath(String relativePath) {
		return null;
	}
}
