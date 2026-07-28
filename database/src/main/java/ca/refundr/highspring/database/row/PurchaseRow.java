package ca.refundr.highspring.database.row;

import ca.refundr.highspring.database.tables.Tables;
import com.google.common.base.Preconditions;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.impl.DSL;

import java.math.BigDecimal;
import java.sql.Connection;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A saved order: money totals plus each line the shopper bought.
 */
public final class PurchaseRow {

	private final UUID id;
	private final UUID userId;
	private final BigDecimal subtotal;
	private final BigDecimal salesTax;
	private final BigDecimal total;
	private final BigDecimal taxRate;
	private final Instant createdAt;
	private final List<PurchaseItemRow> items;

	public PurchaseRow(UUID id, UUID userId, BigDecimal subtotal, BigDecimal salesTax, BigDecimal total,
		BigDecimal taxRate, Instant createdAt, List<PurchaseItemRow> items) {
		this.id = id;
		this.userId = userId;
		this.subtotal = subtotal;
		this.salesTax = salesTax;
		this.total = total;
		this.taxRate = taxRate;
		this.createdAt = createdAt;
		this.items = List.copyOf(items);
	}

	public UUID getId() {
		return id;
	}

	public UUID getUserId() {
		return userId;
	}

	public BigDecimal getSubtotal() {
		return subtotal;
	}

	public BigDecimal getSalesTax() {
		return salesTax;
	}

	public BigDecimal getTotal() {
		return total;
	}

	public BigDecimal getTaxRate() {
		return taxRate;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public List<PurchaseItemRow> getItems() {
		return items;
	}

	public record PurchaseItemRow(
		UUID productId,
		String productName,
		BigDecimal unitPrice,
		int quantity,
		BigDecimal discountPercent,
		BigDecimal lineSubtotal
	) {
	}

	public record NewItem(
		UUID productId,
		String productName,
		BigDecimal unitPrice,
		int quantity,
		BigDecimal discountPercent,
		BigDecimal lineSubtotal
	) {
	}

	public static PurchaseRow insert(Connection connection, UUID userId, BigDecimal subtotal,
		BigDecimal salesTax, BigDecimal total, BigDecimal taxRate, List<NewItem> items) {
		Preconditions.checkNotNull(userId, "userId");
		Preconditions.checkNotNull(items, "items");
		Preconditions.checkArgument(!items.isEmpty(), "Purchase must contain at least one item");

		DSLContext dsl = DSL.using(connection);
		UUID purchaseId = UUID.randomUUID();
		OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

		dsl.insertInto(Tables.PURCHASE.table)
			.set(Tables.PURCHASE.ID, purchaseId)
			.set(Tables.PURCHASE.USER_ID, userId)
			.set(Tables.PURCHASE.SUBTOTAL, subtotal)
			.set(Tables.PURCHASE.SALES_TAX, salesTax)
			.set(Tables.PURCHASE.TOTAL, total)
			.set(Tables.PURCHASE.TAX_RATE, taxRate)
			.set(Tables.PURCHASE.CREATED_AT, now)
			.execute();

		for (NewItem item : items) {
			dsl.insertInto(Tables.PURCHASE_ITEM.table)
				.set(Tables.PURCHASE_ITEM.ID, UUID.randomUUID())
				.set(Tables.PURCHASE_ITEM.PURCHASE_ID, purchaseId)
				.set(Tables.PURCHASE_ITEM.PRODUCT_ID, item.productId())
				.set(Tables.PURCHASE_ITEM.PRODUCT_NAME, item.productName())
				.set(Tables.PURCHASE_ITEM.UNIT_PRICE, item.unitPrice())
				.set(Tables.PURCHASE_ITEM.QUANTITY, item.quantity())
				.set(Tables.PURCHASE_ITEM.DISCOUNT_PERCENT, item.discountPercent())
				.set(Tables.PURCHASE_ITEM.LINE_SUBTOTAL, item.lineSubtotal())
				.execute();
		}

		return fetchById(dsl, purchaseId);
	}

	public static PurchaseRow fetchById(DSLContext dsl, UUID id) {
		Record purchase = dsl.selectFrom(Tables.PURCHASE.table)
			.where(Tables.PURCHASE.ID.eq(id))
			.fetchOne();
		if (purchase == null) {
			return null;
		}
		List<PurchaseItemRow> items = dsl.selectFrom(Tables.PURCHASE_ITEM.table)
			.where(Tables.PURCHASE_ITEM.PURCHASE_ID.eq(id))
			.fetch(r -> new PurchaseItemRow(
				r.get(Tables.PURCHASE_ITEM.PRODUCT_ID),
				r.get(Tables.PURCHASE_ITEM.PRODUCT_NAME),
				r.get(Tables.PURCHASE_ITEM.UNIT_PRICE),
				r.get(Tables.PURCHASE_ITEM.QUANTITY),
				r.get(Tables.PURCHASE_ITEM.DISCOUNT_PERCENT),
				r.get(Tables.PURCHASE_ITEM.LINE_SUBTOTAL)
			));
		return new PurchaseRow(
			purchase.get(Tables.PURCHASE.ID),
			purchase.get(Tables.PURCHASE.USER_ID),
			purchase.get(Tables.PURCHASE.SUBTOTAL),
			purchase.get(Tables.PURCHASE.SALES_TAX),
			purchase.get(Tables.PURCHASE.TOTAL),
			purchase.get(Tables.PURCHASE.TAX_RATE),
			purchase.get(Tables.PURCHASE.CREATED_AT).toInstant(),
			items
		);
	}

	public static List<PurchaseRow> listRecent(DSLContext dsl, int limit) {
		List<UUID> ids = dsl.select(Tables.PURCHASE.ID)
			.from(Tables.PURCHASE.table)
			.orderBy(Tables.PURCHASE.CREATED_AT.desc())
			.limit(limit)
			.fetch(Tables.PURCHASE.ID);
		List<PurchaseRow> result = new ArrayList<>();
		for (UUID id : ids) {
			result.add(fetchById(dsl, id));
		}
		return result;
	}

	public static long countAll(DSLContext dsl) {
		return dsl.fetchCount(Tables.PURCHASE.table);
	}

	public static BigDecimal sumField(DSLContext dsl, org.jooq.Field<BigDecimal> field) {
		BigDecimal sum = dsl.select(DSL.coalesce(DSL.sum(field), BigDecimal.ZERO))
			.from(Tables.PURCHASE.table)
			.fetchOne(0, BigDecimal.class);
		return sum == null ? BigDecimal.ZERO : sum;
	}
}
