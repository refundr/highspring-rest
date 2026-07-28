package ca.refundr.highspring.api.resource.version1.admin;

import ca.refundr.highspring.api.resource.AbstractChildResource;
import ca.refundr.highspring.api.resource.AbstractResource;
import ca.refundr.highspring.api.scope.RequestScope;
import ca.refundr.highspring.api.util.ServerResponse;
import ca.refundr.highspring.database.row.PurchaseRow;
import ca.refundr.highspring.database.tables.Tables;
import ca.refundr.highspring.domain.admin.AdminTotalsResponse;
import org.eclipse.jetty.http.HttpMethod;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;

import static org.eclipse.jetty.http.HttpStatus.METHOD_NOT_ALLOWED_405;
import static org.eclipse.jetty.http.HttpStatus.OK_200;

/**
 * Sales summary numbers for the admin dashboard.
 */
public final class AdminTotalsResource extends AbstractChildResource<AdminResource> {

	public AdminTotalsResource(RequestScope scope, AdminResource parent) {
		super(scope, parent);
		supportedMethods.add(HttpMethod.GET.asString());
	}

	@Override
	public String getRelativePath() {
		return "totals/";
	}

	@Override
	public ServerResponse httpGet() {
		if (!scope.getRequest().acceptsJson()) {
			return writer -> writer.sendEmpty(METHOD_NOT_ALLOWED_405);
		}
		requireAdmin();
		AdminTotalsResponse totals = scope.getDatabase().transactionWithResult(connection -> {
			DSLContext dsl = DSL.using(connection);
			return new AdminTotalsResponse(
				PurchaseRow.countAll(dsl),
				PurchaseRow.sumField(dsl, Tables.PURCHASE.TOTAL),
				PurchaseRow.sumField(dsl, Tables.PURCHASE.SALES_TAX),
				PurchaseRow.sumField(dsl, Tables.PURCHASE.SUBTOTAL)
			);
		});
		return writer -> writer.sendJson(OK_200, totals);
	}

	@Override
	protected <T extends AbstractResource> T getDescendantByPath(String relativePath) {
		return null;
	}
}
