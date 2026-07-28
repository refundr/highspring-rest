package ca.refundr.highspring.api.resource.version1.cart;

import ca.refundr.highspring.api.pricing.CartPricingService;
import ca.refundr.highspring.api.resource.AbstractChildResource;
import ca.refundr.highspring.api.resource.AbstractResource;
import ca.refundr.highspring.api.resource.version1.Version1Resource;
import ca.refundr.highspring.api.resource.version1.purchase.PurchasesResource;
import ca.refundr.highspring.api.scope.RequestScope;
import ca.refundr.highspring.api.util.ServerResponse;
import ca.refundr.highspring.api.util.exceptions.BadRequestException;
import ca.refundr.highspring.database.row.AppUserRow;
import ca.refundr.highspring.database.row.CartItemRow;
import ca.refundr.highspring.database.row.ProductRow;
import ca.refundr.highspring.database.row.PurchaseRow;
import ca.refundr.highspring.domain.cart.AddCartItemRequest;
import ca.refundr.highspring.domain.cart.CartResponse;
import ca.refundr.highspring.domain.cart.UpsertCartItemRequest;
import ca.refundr.highspring.domain.purchase.PurchaseResponse;
import org.eclipse.jetty.http.HttpMethod;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.eclipse.jetty.http.HttpStatus.BAD_REQUEST_400;
import static org.eclipse.jetty.http.HttpStatus.CREATED_201;
import static org.eclipse.jetty.http.HttpStatus.METHOD_NOT_ALLOWED_405;
import static org.eclipse.jetty.http.HttpStatus.NO_CONTENT_204;
import static org.eclipse.jetty.http.HttpStatus.OK_200;

/**
 * Server-side shopping cart for the signed-in user. Survives browser sessions until checkout.
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

	/**
	 * /v1/cart/items/ — add or set quantities.
	 */
	public static final class CartItemsResource extends AbstractChildResource<CartResource> {

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

	/**
	 * /v1/cart/checkout/ — turn the saved cart into a purchase and empty the cart.
	 */
	public static final class CartCheckoutResource extends AbstractChildResource<CartResource> {

		public CartCheckoutResource(RequestScope scope, CartResource parent) {
			super(scope, parent);
			supportedMethods.add(HttpMethod.POST.asString());
		}

		@Override
		public String getRelativePath() {
			return "checkout/";
		}

		@Override
		public ServerResponse httpPost() {
			if (!scope.getRequest().acceptsJson()) {
				return writer -> writer.sendEmpty(METHOD_NOT_ALLOWED_405);
			}
			AppUserRow user = requireSessionUser();
			PurchaseResponse response = scope.getDatabase().transactionWithResult(connection -> {
				DSLContext dsl = DSL.using(connection);
				List<CartItemRow> rows = CartItemRow.listForUser(dsl, user.getId());
				if (rows.isEmpty()) {
					throw new BadRequestException("Cart is empty");
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
				List<PurchaseRow.NewItem> newItems = totals.lines().stream()
					.map(line -> new PurchaseRow.NewItem(
						line.productId(),
						line.productName(),
						line.unitPrice(),
						line.quantity(),
						line.discountPercent(),
						line.lineSubtotal()
					))
					.toList();
				PurchaseRow purchase = PurchaseRow.insert(
					connection,
					user.getId(),
					totals.subtotal(),
					totals.salesTax(),
					totals.total(),
					totals.taxRate(),
					newItems
				);
				CartItemRow.clearForUser(connection, user.getId());
				return PurchasesResource.toResponse(purchase);
			});
			return writer -> writer.sendJson(CREATED_201, response);
		}

		@Override
		protected <T extends AbstractResource> T getDescendantByPath(String relativePath) {
			return null;
		}
	}
}
