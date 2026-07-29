package ca.refundr.highspring.domain.admin;

import java.math.BigDecimal;

/**
 * High-level sales numbers for the admin dashboard.
 *
 * @param purchaseCount      how many purchases have been recorded
 * @param totalRevenue       sum of purchase totals (includes tax)
 * @param totalTaxCollected  sum of sales tax across purchases
 * @param totalSubtotal      sum of purchase subtotals (before tax)
 */
public record AdminTotalsResponse(
	long purchaseCount,
	BigDecimal totalRevenue,
	BigDecimal totalTaxCollected,
	BigDecimal totalSubtotal
) {
}
