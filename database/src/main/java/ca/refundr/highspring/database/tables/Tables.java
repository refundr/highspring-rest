package ca.refundr.highspring.database.tables;

import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Table;
import org.jooq.impl.DSL;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.jooq.impl.DSL.name;

/**
 * Named table/column handles for JOOQ queries (no generated schema classes required).
 */
public final class Tables {

	private Tables() {
	}

	public static final CategoryTable CATEGORY = new CategoryTable();
	public static final ProductTable PRODUCT = new ProductTable();
	public static final AppUserTable APP_USER = new AppUserTable();
	public static final ApiSessionTable API_SESSION = new ApiSessionTable();
	public static final PurchaseTable PURCHASE = new PurchaseTable();
	public static final PurchaseItemTable PURCHASE_ITEM = new PurchaseItemTable();
	public static final ApiErrorLogTable API_ERROR_LOG = new ApiErrorLogTable();

	public static final class CategoryTable {
		public final Table<Record> table = DSL.table(name("category"));
		public final Field<UUID> ID = DSL.field(name("category", "id"), UUID.class);
		public final Field<String> CODE = DSL.field(name("category", "code"), String.class);
		public final Field<String> NAME = DSL.field(name("category", "name"), String.class);
		public final Field<BigDecimal> DISCOUNT_PERCENT = DSL.field(name("category", "discount_percent"), BigDecimal.class);
	}

	public static final class ProductTable {
		public final Table<Record> table = DSL.table(name("product"));
		public final Field<UUID> ID = DSL.field(name("product", "id"), UUID.class);
		public final Field<UUID> CATEGORY_ID = DSL.field(name("product", "category_id"), UUID.class);
		public final Field<String> NAME = DSL.field(name("product", "name"), String.class);
		public final Field<BigDecimal> UNIT_PRICE = DSL.field(name("product", "unit_price"), BigDecimal.class);
		public final Field<Boolean> ACTIVE = DSL.field(name("product", "active"), Boolean.class);
	}

	public static final class AppUserTable {
		public final Table<Record> table = DSL.table(name("app_user"));
		public final Field<UUID> ID = DSL.field(name("app_user", "id"), UUID.class);
		public final Field<String> GOOGLE_SUB = DSL.field(name("app_user", "google_sub"), String.class);
		public final Field<String> EMAIL = DSL.field(name("app_user", "email"), String.class);
		public final Field<String> DISPLAY_NAME = DSL.field(name("app_user", "display_name"), String.class);
		public final Field<String> ROLE = DSL.field(name("app_user", "role"), String.class);
		public final Field<OffsetDateTime> CREATED_AT = DSL.field(name("app_user", "created_at"), OffsetDateTime.class);
		public final Field<OffsetDateTime> UPDATED_AT = DSL.field(name("app_user", "updated_at"), OffsetDateTime.class);
	}

	public static final class ApiSessionTable {
		public final Table<Record> table = DSL.table(name("api_session"));
		public final Field<UUID> ID = DSL.field(name("api_session", "id"), UUID.class);
		public final Field<UUID> USER_ID = DSL.field(name("api_session", "user_id"), UUID.class);
		public final Field<OffsetDateTime> EXPIRES_AT = DSL.field(name("api_session", "expires_at"), OffsetDateTime.class);
		public final Field<OffsetDateTime> LAST_ACTIVITY_AT = DSL.field(name("api_session", "last_activity_at"), OffsetDateTime.class);
		public final Field<OffsetDateTime> CREATED_AT = DSL.field(name("api_session", "created_at"), OffsetDateTime.class);
	}

	public static final class PurchaseTable {
		public final Table<Record> table = DSL.table(name("purchase"));
		public final Field<UUID> ID = DSL.field(name("purchase", "id"), UUID.class);
		public final Field<UUID> USER_ID = DSL.field(name("purchase", "user_id"), UUID.class);
		public final Field<BigDecimal> SUBTOTAL = DSL.field(name("purchase", "subtotal"), BigDecimal.class);
		public final Field<BigDecimal> SALES_TAX = DSL.field(name("purchase", "sales_tax"), BigDecimal.class);
		public final Field<BigDecimal> TOTAL = DSL.field(name("purchase", "total"), BigDecimal.class);
		public final Field<BigDecimal> TAX_RATE = DSL.field(name("purchase", "tax_rate"), BigDecimal.class);
		public final Field<OffsetDateTime> CREATED_AT = DSL.field(name("purchase", "created_at"), OffsetDateTime.class);
	}

	public static final class PurchaseItemTable {
		public final Table<Record> table = DSL.table(name("purchase_item"));
		public final Field<UUID> ID = DSL.field(name("purchase_item", "id"), UUID.class);
		public final Field<UUID> PURCHASE_ID = DSL.field(name("purchase_item", "purchase_id"), UUID.class);
		public final Field<UUID> PRODUCT_ID = DSL.field(name("purchase_item", "product_id"), UUID.class);
		public final Field<String> PRODUCT_NAME = DSL.field(name("purchase_item", "product_name"), String.class);
		public final Field<BigDecimal> UNIT_PRICE = DSL.field(name("purchase_item", "unit_price"), BigDecimal.class);
		public final Field<Integer> QUANTITY = DSL.field(name("purchase_item", "quantity"), Integer.class);
		public final Field<BigDecimal> DISCOUNT_PERCENT = DSL.field(name("purchase_item", "discount_percent"), BigDecimal.class);
		public final Field<BigDecimal> LINE_SUBTOTAL = DSL.field(name("purchase_item", "line_subtotal"), BigDecimal.class);
	}

	public static final class ApiErrorLogTable {
		public final Table<Record> table = DSL.table(name("api_error_log"));
		public final Field<Long> ID = DSL.field(name("api_error_log", "id"), Long.class);
		public final Field<String> LEVEL = DSL.field(name("api_error_log", "level"), String.class);
		public final Field<String> LOGGER_NAME = DSL.field(name("api_error_log", "logger_name"), String.class);
		public final Field<String> MESSAGE = DSL.field(name("api_error_log", "message"), String.class);
		public final Field<String> STACK_TRACE = DSL.field(name("api_error_log", "stack_trace"), String.class);
		public final Field<String> REQUEST_METHOD = DSL.field(name("api_error_log", "request_method"), String.class);
		public final Field<String> REQUEST_PATH = DSL.field(name("api_error_log", "request_path"), String.class);
		public final Field<UUID> USER_ID = DSL.field(name("api_error_log", "user_id"), UUID.class);
		public final Field<OffsetDateTime> CREATED_AT = DSL.field(name("api_error_log", "created_at"), OffsetDateTime.class);
	}
}
