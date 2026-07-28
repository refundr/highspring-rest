package ca.refundr.highspring.domain.purchase;

import java.util.List;
import java.util.UUID;

/**
 * What the shopper wants to buy: product ids and quantities.
 */
public record CreatePurchaseRequest(List<PurchaseLineRequest> items) {
	public record PurchaseLineRequest(UUID productId, int quantity) {
	}
}
