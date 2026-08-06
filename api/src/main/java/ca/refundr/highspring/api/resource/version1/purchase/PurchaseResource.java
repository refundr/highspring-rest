package ca.refundr.highspring.api.resource.version1.purchase;

import ca.refundr.highspring.api.resource.AbstractChildResource;
import ca.refundr.highspring.api.resource.AbstractResource;
import ca.refundr.highspring.api.scope.RequestScope;
import ca.refundr.highspring.api.util.ServerResponse;
import ca.refundr.highspring.database.model.UserRole;
import ca.refundr.highspring.database.row.AppUserRow;
import ca.refundr.highspring.database.row.PurchaseRow;
import ca.refundr.highspring.domain.purchase.PurchaseResponse;
import org.eclipse.jetty.http.HttpMethod;
import org.jooq.impl.DSL;

import java.util.UUID;

import static org.eclipse.jetty.http.HttpStatus.FORBIDDEN_403;
import static org.eclipse.jetty.http.HttpStatus.METHOD_NOT_ALLOWED_405;
import static org.eclipse.jetty.http.HttpStatus.NOT_FOUND_404;
import static org.eclipse.jetty.http.HttpStatus.OK_200;

/**
 * A single saved purchase ({@code /v1/purchases/{id}/}).
 */
public final class PurchaseResource extends AbstractChildResource<PurchasesResource> {

	private final UUID purchaseId;

	public PurchaseResource(RequestScope scope, PurchasesResource parent, UUID purchaseId) {
		super(scope, parent);
		this.purchaseId = purchaseId;
		supportedMethods.add(HttpMethod.GET.asString());
	}

	@Override
	public String getRelativePath() {
		return purchaseId + "/";
	}

	@Override
	public ServerResponse httpGet() {
		if (!scope.getRequest().acceptsJson()) {
			return writer -> writer.sendEmpty(METHOD_NOT_ALLOWED_405);
		}
		AppUserRow user = requireSessionUser();
		PurchaseRow purchase = scope.getDatabase().transactionWithResult(connection ->
			PurchaseRow.fetchById(DSL.using(connection), purchaseId)
		);
		if (purchase == null) {
			return writer -> writer.sendText(NOT_FOUND_404, "Purchase not found");
		}
		if (!purchase.getUserId().equals(user.getId()) && user.getRole() != UserRole.ADMIN) {
			return writer -> writer.sendText(FORBIDDEN_403, "You do not have permission.");
		}
		PurchaseResponse response = PurchasesResource.toResponse(purchase);
		return writer -> writer.sendJson(OK_200, response);
	}

	@Override
	protected <T extends AbstractResource> T getDescendantByPath(String relativePath) {
		return null;
	}
}
