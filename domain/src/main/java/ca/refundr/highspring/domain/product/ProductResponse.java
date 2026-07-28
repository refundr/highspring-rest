package ca.refundr.highspring.domain.product;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * A product a shopper can add to a purchase, including its category discount.
 */
public record ProductResponse(
	UUID id,
	String name,
	BigDecimal unitPrice,
	String imageUrl,
	UUID categoryId,
	String categoryCode,
	String categoryName,
	BigDecimal discountPercent
) {
}
