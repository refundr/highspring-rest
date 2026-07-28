package ca.refundr.highspring.database.row;

import ca.refundr.highspring.database.tables.Tables;
import org.jooq.DSLContext;
import org.jooq.Record;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * A sellable item and the discount that belongs to its category.
 */
public final class ProductRow {

	private final UUID id;
	private final String name;
	private final BigDecimal unitPrice;
	private final String imageUrl;
	private final UUID categoryId;
	private final String categoryCode;
	private final String categoryName;
	private final BigDecimal discountPercent;

	public ProductRow(UUID id, String name, BigDecimal unitPrice, String imageUrl, UUID categoryId, String categoryCode,
		String categoryName, BigDecimal discountPercent) {
		this.id = id;
		this.name = name;
		this.unitPrice = unitPrice;
		this.imageUrl = imageUrl;
		this.categoryId = categoryId;
		this.categoryCode = categoryCode;
		this.categoryName = categoryName;
		this.discountPercent = discountPercent;
	}

	public UUID getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public BigDecimal getUnitPrice() {
		return unitPrice;
	}

	public String getImageUrl() {
		return imageUrl;
	}

	public UUID getCategoryId() {
		return categoryId;
	}

	public String getCategoryCode() {
		return categoryCode;
	}

	public String getCategoryName() {
		return categoryName;
	}

	public BigDecimal getDiscountPercent() {
		return discountPercent;
	}

	public static List<ProductRow> listActive(DSLContext dsl) {
		return dsl.select(
				Tables.PRODUCT.ID,
				Tables.PRODUCT.NAME,
				Tables.PRODUCT.UNIT_PRICE,
				Tables.PRODUCT.IMAGE_URL,
				Tables.PRODUCT.CATEGORY_ID,
				Tables.CATEGORY.CODE,
				Tables.CATEGORY.NAME,
				Tables.CATEGORY.DISCOUNT_PERCENT
			)
			.from(Tables.PRODUCT.table)
			.join(Tables.CATEGORY.table).on(Tables.PRODUCT.CATEGORY_ID.eq(Tables.CATEGORY.ID))
			.where(Tables.PRODUCT.ACTIVE.isTrue())
			.orderBy(Tables.PRODUCT.NAME.asc())
			.fetch(record -> fromRecord(record));
	}

	public static ProductRow fetchActiveById(DSLContext dsl, UUID id) {
		Record record = dsl.select(
				Tables.PRODUCT.ID,
				Tables.PRODUCT.NAME,
				Tables.PRODUCT.UNIT_PRICE,
				Tables.PRODUCT.IMAGE_URL,
				Tables.PRODUCT.CATEGORY_ID,
				Tables.CATEGORY.CODE,
				Tables.CATEGORY.NAME,
				Tables.CATEGORY.DISCOUNT_PERCENT
			)
			.from(Tables.PRODUCT.table)
			.join(Tables.CATEGORY.table).on(Tables.PRODUCT.CATEGORY_ID.eq(Tables.CATEGORY.ID))
			.where(Tables.PRODUCT.ID.eq(id))
			.and(Tables.PRODUCT.ACTIVE.isTrue())
			.fetchOne();
		if (record == null) {
			return null;
		}
		return fromRecord(record);
	}

	private static ProductRow fromRecord(Record record) {
		return new ProductRow(
			record.get(Tables.PRODUCT.ID),
			record.get(Tables.PRODUCT.NAME),
			record.get(Tables.PRODUCT.UNIT_PRICE),
			record.get(Tables.PRODUCT.IMAGE_URL),
			record.get(Tables.PRODUCT.CATEGORY_ID),
			record.get(Tables.CATEGORY.CODE),
			record.get(Tables.CATEGORY.NAME),
			record.get(Tables.CATEGORY.DISCOUNT_PERCENT)
		);
	}
}
