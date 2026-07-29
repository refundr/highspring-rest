package ca.refundr.highspring.domain.cart;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * The shopper's saved cart with priced lines and running totals.
 *
 * @param items      priced lines currently in the cart
 * @param subtotal   sum of line totals after category discounts, before tax
 * @param salesTax   tax on {@code subtotal} at {@code taxRate}
 * @param total      {@code subtotal + salesTax} — amount due
 * @param taxRate    sales-tax rate used for this calculation (e.g. 0.085)
 * @param itemCount  sum of quantities across all lines
 */
public record CartResponse(
	List<CartItemResponse> items,
	BigDecimal subtotal,
	BigDecimal salesTax,
	BigDecimal total,
	BigDecimal taxRate,
	int itemCount
) {
	/**
	 * One product line in the cart after pricing.
	 *
	 * @param productId        catalog product id
	 * @param productName      name snapshot for display
	 * @param imageUrl         product image URL
	 * @param unitPrice        unit list price used for this line
	 * @param quantity         how many units
	 * @param discountPercent  category discount applied to this line (0–100)
	 * @param lineSubtotal     {@code unitPrice × quantity} after discount, before tax
	 */
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
