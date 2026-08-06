package ca.refundr.highspring.api.resource.version1.cart;

import ca.refundr.highspring.api.resource.AbstractChildResource;
import ca.refundr.highspring.api.resource.AbstractResource;
import ca.refundr.highspring.api.resource.version1.purchase.PurchasesResource;
import ca.refundr.highspring.api.scope.RequestScope;
import ca.refundr.highspring.api.util.ServerResponse;
import ca.refundr.highspring.database.row.AppUserRow;
import ca.refundr.highspring.database.row.CartItemRow;
import ca.refundr.highspring.database.row.ProductRow;
import ca.refundr.highspring.domain.cart.AddCartItemRequest;
import ca.refundr.highspring.domain.cart.CartResponse;
import ca.refundr.highspring.domain.cart.UpsertCartItemRequest;
import org.eclipse.jetty.http.HttpMethod;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;

import java.io.IOException;

import static org.eclipse.jetty.http.HttpStatus.BAD_REQUEST_400;
import static org.eclipse.jetty.http.HttpStatus.METHOD_NOT_ALLOWED_405;
import static org.eclipse.jetty.http.HttpStatus.OK_200;

/**
 * {@code /v1/cart/items/} — add or set quantities.
 */
public final class CartItemsResource extends AbstractChildResource<CartResource> {

	public CartItemsResource(RequestScope scope, CartResource parent) {
		super(scope, parent);
		supportedMethods.add(HttpMethod.POST.asString());
		supportedMethods.add(HttpMethod.PUT.asString());
	}

	@Override
	public String getRelativePath() {
		return "items/";
	}

	@Override
	public ServerResponse httpPost() throws IOException {
		if (!scope.getRequest().acceptsJson()) {
			return writer -> writer.sendEmpty(METHOD_NOT_ALLOWED_405);
		}
		AppUserRow user = requireSessionUser();
		AddCartItemRequest body = scope.getRequest().getBody(AddCartItemRequest.class);
		if (body == null || body.productId() == null || body.quantity() <= 0) {
			return writer -> writer.sendText(BAD_REQUEST_400, "productId and quantity > 0 are required");
		}
		CartResponse cart = scope.getDatabase().transactionWithResult(connection -> {
			DSLContext dsl = DSL.using(connection);
			ProductRow product = ProductRow.fetchActiveById(dsl, body.productId());
			if (product == null) {
				throw new PurchasesResource.ProductNotFound(body.productId());
			}
			CartItemRow.add(connection, user.getId(), body.productId(), body.quantity());
			return parent.buildCart(dsl, user.getId());
		});
		return writer -> writer.sendJson(OK_200, cart);
	}

	@Override
	public ServerResponse httpPut() throws IOException {
		if (!scope.getRequest().acceptsJson()) {
			return writer -> writer.sendEmpty(METHOD_NOT_ALLOWED_405);
		}
		AppUserRow user = requireSessionUser();
		UpsertCartItemRequest body = scope.getRequest().getBody(UpsertCartItemRequest.class);
		if (body == null || body.productId() == null) {
			return writer -> writer.sendText(BAD_REQUEST_400, "productId is required");
		}
		if (body.quantity() < 0) {
			return writer -> writer.sendText(BAD_REQUEST_400, "quantity cannot be negative");
		}
		CartResponse cart = scope.getDatabase().transactionWithResult(connection -> {
			DSLContext dsl = DSL.using(connection);
			if (body.quantity() > 0) {
				ProductRow product = ProductRow.fetchActiveById(dsl, body.productId());
				if (product == null) {
					throw new PurchasesResource.ProductNotFound(body.productId());
				}
			}
			CartItemRow.upsert(connection, user.getId(), body.productId(), body.quantity());
			return parent.buildCart(dsl, user.getId());
		});
		return writer -> writer.sendJson(OK_200, cart);
	}

	@Override
	protected <T extends AbstractResource> T getDescendantByPath(String relativePath) {
		return null;
	}
}
