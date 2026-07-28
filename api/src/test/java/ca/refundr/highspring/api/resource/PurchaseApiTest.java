package ca.refundr.highspring.api.resource;

import ca.refundr.highspring.api.test.TestSupport;
import ca.refundr.highspring.database.row.ApiErrorLogRow;
import ca.refundr.highspring.database.row.PurchaseRow;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.http.HttpMethod;
import org.jooq.impl.DSL;
import org.testng.annotations.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Epic("Shopping cart API")
@Feature("Purchases and catalog")
public class PurchaseApiTest {

	@Test
	@Story("Happy path checkout")
	@Severity(SeverityLevel.BLOCKER)
	@Description("A signed-in customer can list products and create a purchase with tax totals.")
	public void checkoutHappyPath() throws Exception {
		try (TestSupport test = new TestSupport()) {
			String session = test.loginAs("customer@example.com", "CUSTOMER");

			ContentResponse products = test.request(HttpMethod.GET, "/v1/products/", session, null);
			assertThat(products.getStatus()).isEqualTo(200);
			assertThat(products.getContentAsString()).contains("Wireless Headphones");

			String body = """
				{"items":[{"productId":"aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa","quantity":1},
				{"productId":"eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee","quantity":2}]}
				""";
			ContentResponse created = test.request(HttpMethod.POST, "/v1/purchases/", session, body);
			assertThat(created.getStatus()).isEqualTo(201);
			assertThat(created.getContentAsString()).contains("subtotal");
			assertThat(created.getContentAsString()).contains("salesTax");
			assertThat(created.getContentAsString()).contains("total");
		}
	}

	@Test
	@Story("Unknown product returns 404")
	@Severity(SeverityLevel.CRITICAL)
	public void unknownProductReturns404() throws Exception {
		try (TestSupport test = new TestSupport()) {
			String session = test.loginAs("customer2@example.com", "CUSTOMER");
			String body = """
				{"items":[{"productId":"%s","quantity":1}]}
				""".formatted(UUID.randomUUID());
			ContentResponse created = test.request(HttpMethod.POST, "/v1/purchases/", session, body);
			assertThat(created.getStatus()).isEqualTo(404);
		}
	}

	@Test
	@Story("Unauthorized catalog access")
	@Severity(SeverityLevel.NORMAL)
	public void productsRequireSession() throws Exception {
		try (TestSupport test = new TestSupport()) {
			ContentResponse products = test.request(HttpMethod.GET, "/v1/products/", null, null);
			assertThat(products.getStatus()).isEqualTo(401);
		}
	}

	@Test
	@Story("ACID: failed checkout leaves no purchase")
	@Severity(SeverityLevel.CRITICAL)
	@Description("If every product id is invalid, nothing is saved.")
	public void failedCheckoutLeavesNoPurchase() throws Exception {
		try (TestSupport test = new TestSupport()) {
			String session = test.loginAs("acid@example.com", "CUSTOMER");
			String body = """
				{"items":[{"productId":"%s","quantity":1}]}
				""".formatted(UUID.randomUUID());
			ContentResponse created = test.request(HttpMethod.POST, "/v1/purchases/", session, body);
			assertThat(created.getStatus()).isEqualTo(404);

			long count = test.database().transactionWithResult(connection ->
				PurchaseRow.countAll(DSL.using(connection))
			);
			assertThat(count).isZero();
		}
	}

	@Test
	@Story("500 saves error log")
	@Severity(SeverityLevel.CRITICAL)
	public void boomWritesErrorLog() throws Exception {
		try (TestSupport test = new TestSupport()) {
			String adminSession = test.loginAs("admin@example.com", "ADMIN");
			ContentResponse boom = test.request(HttpMethod.GET, "/v1/admin/boom/", adminSession, null);
			assertThat(boom.getStatus()).isEqualTo(500);

			List<ApiErrorLogRow> errors = test.database().transactionWithResult(connection ->
				ApiErrorLogRow.listRecent(DSL.using(connection), 10)
			);
			assertThat(errors).isNotEmpty();
			assertThat(errors.getFirst().getStackTrace()).contains("Intentional boom");
		}
	}
}
