package ca.refundr.highspring.database.row;

import ca.refundr.highspring.database.tables.Tables;
import org.jooq.DSLContext;
import org.jooq.Record;

import java.math.BigDecimal;
import java.sql.Connection;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * One line in a shopper's saved cart. Quantities live here until checkout.
 */
public final class CartItemRow {

	private final UUID userId;
	private final UUID productId;
	private final int quantity;
	private final String productName;
	private final String imageUrl;
	private final BigDecimal unitPrice;
	private final BigDecimal discountPercent;

	public CartItemRow(UUID userId, UUID productId, int quantity, String productName, String imageUrl,
		BigDecimal unitPrice, BigDecimal discountPercent) {
		this.userId = userId;
		this.productId = productId;
		this.quantity = quantity;
		this.productName = productName;
		this.imageUrl = imageUrl;
		this.unitPrice = unitPrice;
		this.discountPercent = discountPercent;
	}

	public UUID getUserId() {
		return userId;
	}

	public UUID getProductId() {
		return productId;
	}

	public int getQuantity() {
		return quantity;
	}

	public String getProductName() {
		return productName;
	}

	public String getImageUrl() {
		return imageUrl;
	}

	public BigDecimal getUnitPrice() {
		return unitPrice;
	}

	public BigDecimal getDiscountPercent() {
		return discountPercent;
	}

	/**
	 * Sets an absolute quantity for a product. Quantity 0 removes the line.
	 */
	public static void upsert(Connection connection, UUID userId, UUID productId, int quantity) {
		DSLContext dsl = org.jooq.impl.DSL.using(connection);
		if (quantity <= 0) {
			dsl.deleteFrom(Tables.CART_ITEM.table)
				.where(Tables.CART_ITEM.USER_ID.eq(userId))
				.and(Tables.CART_ITEM.PRODUCT_ID.eq(productId))
				.execute();
			return;
		}
		int updated = dsl.update(Tables.CART_ITEM.table)
			.set(Tables.CART_ITEM.QUANTITY, quantity)
			.set(Tables.CART_ITEM.UPDATED_AT, OffsetDateTime.now())
			.where(Tables.CART_ITEM.USER_ID.eq(userId))
			.and(Tables.CART_ITEM.PRODUCT_ID.eq(productId))
			.execute();
		if (updated == 0) {
			dsl.insertInto(Tables.CART_ITEM.table)
				.set(Tables.CART_ITEM.USER_ID, userId)
				.set(Tables.CART_ITEM.PRODUCT_ID, productId)
				.set(Tables.CART_ITEM.QUANTITY, quantity)
				.set(Tables.CART_ITEM.UPDATED_AT, OffsetDateTime.now())
				.execute();
		}
	}

	/**
	 * Adds to an existing quantity (or creates the line).
	 */
	public static void add(Connection connection, UUID userId, UUID productId, int quantityToAdd) {
		if (quantityToAdd <= 0) {
			return;
		}
		DSLContext dsl = org.jooq.impl.DSL.using(connection);
		Record existing = dsl.select(Tables.CART_ITEM.QUANTITY)
			.from(Tables.CART_ITEM.table)
			.where(Tables.CART_ITEM.USER_ID.eq(userId))
			.and(Tables.CART_ITEM.PRODUCT_ID.eq(productId))
			.fetchOne();
		int next = quantityToAdd + (existing == null ? 0 : existing.get(Tables.CART_ITEM.QUANTITY));
		upsert(connection, userId, productId, next);
	}

	public static List<CartItemRow> listForUser(DSLContext dsl, UUID userId) {
		return dsl.select(
				Tables.CART_ITEM.USER_ID,
				Tables.CART_ITEM.PRODUCT_ID,
				Tables.CART_ITEM.QUANTITY,
				Tables.PRODUCT.NAME,
				Tables.PRODUCT.IMAGE_URL,
				Tables.PRODUCT.UNIT_PRICE,
				Tables.CATEGORY.DISCOUNT_PERCENT
			)
			.from(Tables.CART_ITEM.table)
			.join(Tables.PRODUCT.table).on(Tables.CART_ITEM.PRODUCT_ID.eq(Tables.PRODUCT.ID))
			.join(Tables.CATEGORY.table).on(Tables.PRODUCT.CATEGORY_ID.eq(Tables.CATEGORY.ID))
			.where(Tables.CART_ITEM.USER_ID.eq(userId))
			.and(Tables.PRODUCT.ACTIVE.isTrue())
			.orderBy(Tables.PRODUCT.NAME.asc())
			.fetch(record -> new CartItemRow(
				record.get(Tables.CART_ITEM.USER_ID),
				record.get(Tables.CART_ITEM.PRODUCT_ID),
				record.get(Tables.CART_ITEM.QUANTITY),
				record.get(Tables.PRODUCT.NAME),
				record.get(Tables.PRODUCT.IMAGE_URL),
				record.get(Tables.PRODUCT.UNIT_PRICE),
				record.get(Tables.CATEGORY.DISCOUNT_PERCENT)
			));
	}

	public static void clearForUser(Connection connection, UUID userId) {
		org.jooq.impl.DSL.using(connection)
			.deleteFrom(Tables.CART_ITEM.table)
			.where(Tables.CART_ITEM.USER_ID.eq(userId))
			.execute();
	}
}
