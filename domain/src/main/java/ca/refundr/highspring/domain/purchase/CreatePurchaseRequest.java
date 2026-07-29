package ca.refundr.highspring.domain.purchase;

import java.util.List;
import java.util.UUID;

/**
 * What the shopper wants to buy: product ids and quantities.
 *
 * @param items  non-empty list of product lines to purchase
 */
public record CreatePurchaseRequest(List<PurchaseLineRequest> items) {
	/**
	 * One requested line for a direct purchase (not via the saved cart).
	 *
	 * @param productId  catalog product id
	 * @param quantity   units to buy (must be &gt; 0)
	 */
	public record PurchaseLineRequest(UUID productId, int quantity) {
	}
}
