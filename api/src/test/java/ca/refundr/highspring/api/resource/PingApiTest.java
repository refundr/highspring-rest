package ca.refundr.highspring.api.resource;

import ca.refundr.highspring.api.test.TestSupport;
import com.fasterxml.jackson.databind.JsonNode;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.http.HttpMethod;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Epic("Ops")
@Feature("Liveness")
public class PingApiTest {

	@Test
	@Story("GET /ping answers without a session")
	@Severity(SeverityLevel.CRITICAL)
	public void pingReturnsOkWithoutAuth() throws Exception {
		try (TestSupport test = new TestSupport()) {
			ContentResponse response = test.request(HttpMethod.GET, "/ping", null, null);
			assertThat(response.getStatus()).isEqualTo(200);
			JsonNode json = test.mapper().readTree(response.getContentAsString());
			assertThat(json.get("status").asText()).isEqualTo("ok");
		}
	}

	@Test
	@Story("GET /ping/ is the same liveness probe")
	@Severity(SeverityLevel.NORMAL)
	public void pingAcceptsTrailingSlash() throws Exception {
		try (TestSupport test = new TestSupport()) {
			ContentResponse response = test.request(HttpMethod.GET, "/ping/", null, null);
			assertThat(response.getStatus()).isEqualTo(200);
			JsonNode json = test.mapper().readTree(response.getContentAsString());
			assertThat(json.get("status").asText()).isEqualTo("ok");
		}
	}
}
