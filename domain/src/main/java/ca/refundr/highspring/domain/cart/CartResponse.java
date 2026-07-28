package ca.refundr.highspring.domain.cart;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * The shopper's saved cart with priced lines and running totals.
 */
public record CartResponse(
	List<CartItemResponse> items,
	BigDecimal subtotal,
	BigDecimal salesTax,
	BigDecimal total,
	BigDecimal taxRate,
	int itemCount
) {
	public record CartItemResponse(
		UUID productId,
		String productName,
		String imageUrl,
		BigDecimal unitPrice,
		int quantity,
		BigDecimal discountPercent,
		BigDecimal lineSubtotal
	) {
	}
}
