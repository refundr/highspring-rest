package ca.refundr.highspring.api.resource;

import ca.refundr.highspring.api.test.TestSupport;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.http.HttpMethod;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Epic("Shopping cart API")
@Feature("Persisted cart")
public class CartApiTest {

	@Test
	@Story("Cart survives across requests until checkout")
	@Severity(SeverityLevel.BLOCKER)
	@Description("Add items to the server cart, read them back, then checkout and confirm the cart is empty.")
	public void cartPersistsThenClearsOnCheckout() throws Exception {
		try (TestSupport test = new TestSupport()) {
			String session = test.loginAs("cart@example.com", "CUSTOMER");

			ContentResponse empty = test.request(HttpMethod.GET, "/v1/cart/", session, null);
			assertThat(empty.getStatus()).isEqualTo(200);
			assertThat(empty.getContentAsString()).contains("\"itemCount\":0");

			String add = """
				{"productId":"aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa","quantity":2}
				""";
			ContentResponse added = test.request(HttpMethod.POST, "/v1/cart/items/", session, add);
			assertThat(added.getStatus()).isEqualTo(200);
			assertThat(added.getContentAsString()).contains("Wireless Headphones");
			assertThat(added.getContentAsString()).contains("\"itemCount\":2");

			ContentResponse again = test.request(HttpMethod.GET, "/v1/cart/", session, null);
			assertThat(again.getStatus()).isEqualTo(200);
			assertThat(again.getContentAsString()).contains("\"itemCount\":2");

			ContentResponse checkout = test.request(HttpMethod.POST, "/v1/cart/checkout/", session, "{}");
			assertThat(checkout.getStatus()).isEqualTo(201);
			assertThat(checkout.getContentAsString()).contains("subtotal");

			ContentResponse cleared = test.request(HttpMethod.GET, "/v1/cart/", session, null);
			assertThat(cleared.getStatus()).isEqualTo(200);
			assertThat(cleared.getContentAsString()).contains("\"itemCount\":0");
		}
	}

	@Test
	@Story("Set quantity to zero removes a cart line")
	@Severity(SeverityLevel.CRITICAL)
	public void setQuantityZeroRemovesLine() throws Exception {
		try (TestSupport test = new TestSupport()) {
			String session = test.loginAs("cart2@example.com", "CUSTOMER");
			test.request(HttpMethod.POST, "/v1/cart/items/", session, """
				{"productId":"cccccccc-cccc-cccc-cccc-cccccccccccc","quantity":1}
				""");
			ContentResponse removed = test.request(HttpMethod.PUT, "/v1/cart/items/", session, """
				{"productId":"cccccccc-cccc-cccc-cccc-cccccccccccc","quantity":0}
				""");
			assertThat(removed.getStatus()).isEqualTo(200);
			assertThat(removed.getContentAsString()).contains("\"itemCount\":0");
		}
	}
}
