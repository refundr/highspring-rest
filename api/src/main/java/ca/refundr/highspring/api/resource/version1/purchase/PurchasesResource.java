package ca.refundr.highspring.api.resource.version1.purchase;

import ca.refundr.highspring.api.pricing.CartPricingService;
import ca.refundr.highspring.api.resource.AbstractChildResource;
import ca.refundr.highspring.api.resource.AbstractResource;
import ca.refundr.highspring.api.resource.version1.Version1Resource;
import ca.refundr.highspring.api.scope.RequestScope;
import ca.refundr.highspring.api.util.ServerResponse;
import ca.refundr.highspring.api.util.exceptions.BadRequestException;
import ca.refundr.highspring.database.row.AppUserRow;
import ca.refundr.highspring.database.row.ProductRow;
import ca.refundr.highspring.database.row.PurchaseRow;
import ca.refundr.highspring.domain.purchase.CreatePurchaseRequest;
import ca.refundr.highspring.domain.purchase.PurchaseResponse;
import org.eclipse.jetty.http.HttpMethod;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.eclipse.jetty.http.HttpStatus.BAD_REQUEST_400;
import static org.eclipse.jetty.http.HttpStatus.CREATED_201;
import static org.eclipse.jetty.http.HttpStatus.METHOD_NOT_ALLOWED_405;

/**
 * Purchase (order) endpoints under {@code /v1/purchases/}.
 *
 * <p>{@code POST /v1/purchases/} accepts an explicit item list and saves the order in <strong>one
 * JDBC transaction</strong> (ACID). The Remix UI normally uses {@code POST /v1/cart/checkout/}
 * instead; this endpoint remains for tests and direct API clients.
 *
 * <p>{@code GET /v1/purchases/{id}/} loads one order (owner or ADMIN). Nested id routing uses
 * {@link #getDescendantFromChildByUuid}.
 */
public final class PurchasesResource extends AbstractChildResource<Version1Resource> {

	public PurchasesResource(RequestScope scope, Version1Resource parent) {
		super(scope, parent);
		supportedMethods.add(HttpMethod.POST.asString());
		supportedMethods.add(HttpMethod.GET.asString());
	}

	@Override
	public String getRelativePath() {
		return "purchases/";
	}

	@Override
	public ServerResponse httpPost() throws IOException {
		if (!scope.getRequest().acceptsJson()) {
			return writer -> writer.sendEmpty(METHOD_NOT_ALLOWED_405);
		}
		AppUserRow user = requireSessionUser();
		CreatePurchaseRequest body = scope.getRequest().getBody(CreatePurchaseRequest.class);
		if (body == null || body.items() == null || body.items().isEmpty()) {
			return writer -> writer.sendText(BAD_REQUEST_400, "At least one cart item is required");
		}

		PurchaseResponse response = scope.getDatabase().transactionWithResult(connection -> {
			DSLContext dsl = DSL.using(connection);
			List<CartPricingService.PricedLineInput> inputs = new ArrayList<>();
			for (CreatePurchaseRequest.PurchaseLineRequest line : body.items()) {
				if (line == null || line.productId() == null || line.quantity() <= 0) {
					throw new BadRequestException("Each item needs a productId and quantity > 0");
				}
				ProductRow product = ProductRow.fetchActiveById(dsl, line.productId());
				if (product == null) {
					throw new ProductNotFound(line.productId());
				}
				inputs.add(new CartPricingService.PricedLineInput(
					product.getId(),
					product.getName(),
					product.getUnitPrice(),
					line.quantity(),
					product.getDiscountPercent()
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
			return toResponse(purchase);
		});

		return writer -> writer.sendJson(CREATED_201, response);
	}

	@Override
	protected <T extends AbstractResource> T getDescendantByPath(String relativePath) {
		if (relativePath.isEmpty() || relativePath.equals("/")) {
			return null;
		}
		return getDescendantFromChildByUuid(relativePath, id -> new PurchaseResource(scope, this, id));
	}

	public static PurchaseResponse toResponse(PurchaseRow purchase) {
		return new PurchaseResponse(
			purchase.getId(),
			purchase.getUserId(),
			purchase.getSubtotal(),
			purchase.getSalesTax(),
			purchase.getTotal(),
			purchase.getTaxRate(),
			purchase.getCreatedAt(),
			purchase.getItems().stream()
				.map(item -> new PurchaseResponse.PurchaseItemResponse(
					item.productId(),
					item.productName(),
					item.unitPrice(),
					item.quantity(),
					item.discountPercent(),
					item.lineSubtotal()
				))
				.toList()
		);
	}

	/**
	 * Signals a missing product inside a transaction so the filter can map it to HTTP 404.
	 */
	public static final class ProductNotFound extends RuntimeException {
		private final UUID productId;

		public ProductNotFound(UUID productId) {
			super("Product not found: " + productId);
			this.productId = productId;
		}

		public UUID getProductId() {
			return productId;
		}
	}
}
