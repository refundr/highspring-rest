package ca.refundr.highspring.api.resource.version1.cart;

import ca.refundr.highspring.api.pricing.CartPricingService;
import ca.refundr.highspring.api.resource.AbstractChildResource;
import ca.refundr.highspring.api.resource.AbstractResource;
import ca.refundr.highspring.api.resource.version1.Version1Resource;
import ca.refundr.highspring.api.scope.RequestScope;
import ca.refundr.highspring.api.util.ServerResponse;
import ca.refundr.highspring.database.row.AppUserRow;
import ca.refundr.highspring.database.row.CartItemRow;
import ca.refundr.highspring.domain.cart.CartResponse;
import org.eclipse.jetty.http.HttpMethod;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.eclipse.jetty.http.HttpStatus.METHOD_NOT_ALLOWED_405;
import static org.eclipse.jetty.http.HttpStatus.NO_CONTENT_204;
import static org.eclipse.jetty.http.HttpStatus.OK_200;

/**
 * Server-side shopping cart for the signed-in user ({@code /v1/cart/…}).
 *
 * <p>Industry pattern: once the shopper has an account/session, cart lines live in the database
 * ({@code cart_item}), not only in the browser. That way quantities survive refresh and new devices.
 *
 * <pre>
 *   GET    /v1/cart/            read cart + priced totals
 *   DELETE /v1/cart/            empty the cart
 *   POST   /v1/cart/items/      add quantity (increment)
 *   PUT    /v1/cart/items/      set absolute quantity (0 removes)
 *   POST   /v1/cart/checkout/   create purchase, clear cart (UI payment flow)
 * </pre>
 *
 * <p>Money math is delegated to {@link CartPricingService} (category discount, then tax).
 */
public final class CartResource extends AbstractChildResource<Version1Resource> {

	public CartResource(RequestScope scope, Version1Resource parent) {
		super(scope, parent);
		supportedMethods.add(HttpMethod.GET.asString());
		supportedMethods.add(HttpMethod.DELETE.asString());
	}

	@Override
	public String getRelativePath() {
		return "cart/";
	}

	@Override
	public ServerResponse httpGet() {
		if (!scope.getRequest().acceptsJson()) {
			return writer -> writer.sendEmpty(METHOD_NOT_ALLOWED_405);
		}
		AppUserRow user = requireSessionUser();
		CartResponse cart = scope.getDatabase().transactionWithResult(connection ->
			buildCart(DSL.using(connection), user.getId())
		);
		return writer -> writer.sendJson(OK_200, cart);
	}

	@Override
	public ServerResponse httpDelete() {
		AppUserRow user = requireSessionUser();
		scope.getDatabase().transaction(connection -> CartItemRow.clearForUser(connection, user.getId()));
		return writer -> writer.sendEmpty(NO_CONTENT_204);
	}

	@Override
	protected <T extends AbstractResource> T getDescendantByPath(String relativePath) {
		return getDescendantFromChildren(relativePath, List.of(
			() -> new CartItemsResource(scope, this),
			() -> new CartCheckoutResource(scope, this)
		));
	}

	CartResponse buildCart(DSLContext dsl, UUID userId) {
		List<CartItemRow> rows = CartItemRow.listForUser(dsl, userId);
		if (rows.isEmpty()) {
			return new CartResponse(
				List.of(),
				BigDecimal.ZERO.setScale(2),
				BigDecimal.ZERO.setScale(2),
				BigDecimal.ZERO.setScale(2),
				CartPricingService.DEFAULT_TAX_RATE,
				0
			);
		}
		List<CartPricingService.PricedLineInput> inputs = new ArrayList<>();
		for (CartItemRow row : rows) {
			inputs.add(new CartPricingService.PricedLineInput(
				row.getProductId(),
				row.getProductName(),
				row.getUnitPrice(),
				row.getQuantity(),
				row.getDiscountPercent()
			));
		}
		CartPricingService.CartTotals totals = scope.getCartPricingService().price(inputs);
		List<CartResponse.CartItemResponse> items = new ArrayList<>();
		for (int i = 0; i < totals.lines().size(); i++) {
			CartPricingService.PricedLine line = totals.lines().get(i);
			CartItemRow row = rows.get(i);
			items.add(new CartResponse.CartItemResponse(
				line.productId(),
				line.productName(),
				row.getImageUrl(),
				line.unitPrice(),
				line.quantity(),
				line.discountPercent(),
				line.lineSubtotal()
			));
		}
		int itemCount = rows.stream().mapToInt(CartItemRow::getQuantity).sum();
		return new CartResponse(items, totals.subtotal(), totals.salesTax(), totals.total(), totals.taxRate(), itemCount);
	}
}
