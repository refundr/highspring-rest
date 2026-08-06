package ca.refundr.highspring.api.resource.version1.cart;

import ca.refundr.highspring.api.pricing.CartPricingService;
import ca.refundr.highspring.api.resource.AbstractChildResource;
import ca.refundr.highspring.api.resource.AbstractResource;
import ca.refundr.highspring.api.resource.version1.purchase.PurchasesResource;
import ca.refundr.highspring.api.scope.RequestScope;
import ca.refundr.highspring.api.util.ServerResponse;
import ca.refundr.highspring.api.util.exceptions.BadRequestException;
import ca.refundr.highspring.database.row.AppUserRow;
import ca.refundr.highspring.database.row.CartItemRow;
import ca.refundr.highspring.database.row.PurchaseRow;
import ca.refundr.highspring.domain.purchase.PurchaseResponse;
import org.eclipse.jetty.http.HttpMethod;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;

import java.util.ArrayList;
import java.util.List;

import static org.eclipse.jetty.http.HttpStatus.CREATED_201;
import static org.eclipse.jetty.http.HttpStatus.METHOD_NOT_ALLOWED_405;

/**
 * {@code /v1/cart/checkout/} — turn the saved cart into a purchase and empty the cart.
 */
public final class CartCheckoutResource extends AbstractChildResource<CartResource> {

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
