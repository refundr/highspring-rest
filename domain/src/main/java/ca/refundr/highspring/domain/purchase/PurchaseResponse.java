package ca.refundr.highspring.domain.purchase;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * A finished purchase with money totals after discounts and sales tax.
 *
 * @param id         purchase id
 * @param userId     shopper who owns this purchase
 * @param subtotal   sum of line totals after discounts, before tax
 * @param salesTax   tax charged on {@code subtotal}
 * @param total      {@code subtotal + salesTax} — amount paid
 * @param taxRate    sales-tax rate stored with the purchase
 * @param createdAt  when the purchase was recorded (UTC)
 * @param items      line items frozen at checkout time
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
	/**
	 * One line on a completed purchase (name/price snapshotted at checkout).
	 *
	 * @param productId        catalog product id (may still exist in catalog)
	 * @param productName      name as stored on the purchase line
	 * @param unitPrice        unit list price used at checkout
	 * @param quantity         units purchased
	 * @param discountPercent  category discount applied at checkout (0–100)
	 * @param lineSubtotal     line total after discount, before tax
	 */
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
