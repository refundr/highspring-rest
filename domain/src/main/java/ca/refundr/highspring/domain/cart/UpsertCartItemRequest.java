package ca.refundr.highspring.domain.cart;

import java.util.UUID;

/**
 * Set an absolute quantity for one product in the cart. Quantity 0 removes the line.
 */
public record UpsertCartItemRequest(UUID productId, int quantity) {
}
