package ca.refundr.highspring.domain.cart;

import java.util.UUID;

/**
 * Add more of a product to the cart (increments quantity).
 */
public record AddCartItemRequest(UUID productId, int quantity) {
}
