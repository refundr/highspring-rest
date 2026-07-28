package ca.refundr.highspring.domain.admin;

import java.math.BigDecimal;

/**
 * High-level sales numbers for the admin dashboard.
 */
public record AdminTotalsResponse(
	long purchaseCount,
	BigDecimal totalRevenue,
	BigDecimal totalTaxCollected,
	BigDecimal totalSubtotal
) {
}
