package ca.refundr.highspring.domain.purchase;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * A finished purchase with money totals after discounts and sales tax.
 */
public record PurchaseResponse(
	UUID id,
	UUID userId,
	BigDecimal subtotal,
	BigDecimal salesTax,
	BigDecimal total,
	BigDecimal taxRate,
	Instant createdAt,
	List<PurchaseItemResponse> items
) {
	public record PurchaseItemResponse(
		UUID productId,
		String productName,
		BigDecimal unitPrice,
		int quantity,
		BigDecimal discountPercent,
		BigDecimal lineSubtotal
	) {
	}
}
