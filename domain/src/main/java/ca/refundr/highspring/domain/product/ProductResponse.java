package ca.refundr.highspring.domain.product;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * A product a shopper can add to a purchase, including its category discount.
 *
 * @param id               catalog product id
 * @param name             display name shown in the shop
 * @param unitPrice        list price before category discount (currency units)
 * @param imageUrl         absolute or path URL for the product image
 * @param categoryId       category this product belongs to
 * @param categoryCode     short category code (e.g. for rules/debug)
 * @param categoryName     human-readable category label
 * @param discountPercent  category discount applied before sales tax (0–100)
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
