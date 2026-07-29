package ca.refundr.highspring.api.resource;

import ca.refundr.highspring.api.test.TestSupport;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.http.HttpMethod;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Epic("Auth")
@Feature("Logout")
public class LogoutApiTest {

	@Test
	public void logoutRevokesSession() throws Exception {
		try (TestSupport test = new TestSupport()) {
			String session = test.loginAs("logout@example.com", "CUSTOMER");

			ContentResponse meBefore = test.request(HttpMethod.GET, "/v1/me/", session, null);
			assertThat(meBefore.getStatus()).isEqualTo(200);

			ContentResponse logout = test.request(HttpMethod.DELETE, "/v1/auth/logout/", session, null);
			assertThat(logout.getStatus()).isEqualTo(204);

			ContentResponse meAfter = test.request(HttpMethod.GET, "/v1/me/", session, null);
			assertThat(meAfter.getStatus()).isEqualTo(401);
		}
	}
}
