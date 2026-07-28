package ca.refundr.highspring.api.resource;

import ca.refundr.highspring.api.test.TestSupport;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.http.HttpMethod;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Epic("Admin tooling")
@Feature("Role-gated admin APIs")
public class AdminApiTest {

	@Test
	@Story("Admin can view totals")
	@Severity(SeverityLevel.CRITICAL)
	public void adminTotalsOk() throws Exception {
		try (TestSupport test = new TestSupport()) {
			String admin = test.loginAs("admin@example.com", "ADMIN");
			String customer = test.loginAs("shopper@example.com", "CUSTOMER");

			String body = """
				{"items":[{"productId":"cccccccc-cccc-cccc-cccc-cccccccccccc","quantity":1}]}
				""";
			assertThat(test.request(HttpMethod.POST, "/v1/purchases/", customer, body).getStatus()).isEqualTo(201);

			ContentResponse totals = test.request(HttpMethod.GET, "/v1/admin/totals/", admin, null);
			assertThat(totals.getStatus()).isEqualTo(200);
			assertThat(totals.getContentAsString()).contains("purchaseCount");
		}
	}

	@Test
	@Story("Customer forbidden from admin")
	@Severity(SeverityLevel.CRITICAL)
	public void customerForbiddenFromAdmin() throws Exception {
		try (TestSupport test = new TestSupport()) {
			String customer = test.loginAs("nope@example.com", "CUSTOMER");
			ContentResponse totals = test.request(HttpMethod.GET, "/v1/admin/totals/", customer, null);
			assertThat(totals.getStatus()).isEqualTo(403);
		}
	}

	@Test
	@Story("Admin can open Allure report")
	@Severity(SeverityLevel.CRITICAL)
	public void adminCanOpenAllure() throws Exception {
		try (TestSupport test = new TestSupport()) {
			String admin = test.loginAs("admin@example.com", "ADMIN");
			String customer = test.loginAs("viewer@example.com", "CUSTOMER");

			ContentResponse report = test.request(HttpMethod.GET, "/v1/admin/allure/", admin, null);
			assertThat(report.getStatus()).isEqualTo(200);
			assertThat(report.getContentAsString()).contains("Allure Test Report");

			ContentResponse denied = test.request(HttpMethod.GET, "/v1/admin/allure/", customer, null);
			assertThat(denied.getStatus()).isEqualTo(403);
		}
	}
}
