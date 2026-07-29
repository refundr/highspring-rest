package ca.refundr.highspring.domain.cart;

import java.util.UUID;

/**
 * Add more of a product to the cart (increments quantity).
 *
 * @param productId  catalog product to add
 * @param quantity   how many units to add (must be &gt; 0)
 */
public record AddCartItemRequest(UUID productId, int quantity) {
}
