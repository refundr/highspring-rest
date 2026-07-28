package ca.refundr.highspring.api.test;

import ca.refundr.highspring.api.jetty.JettyServer;
import ca.refundr.highspring.api.oauth.OAuthProvider;
import ca.refundr.highspring.api.oauth.StubOAuthProvider;
import ca.refundr.highspring.api.scope.ServerScope;
import ca.refundr.highspring.common.config.AppConfiguration;
import ca.refundr.highspring.database.scope.TestDatabaseScope;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.util.StringRequestContent;
import org.eclipse.jetty.http.HttpMethod;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.UUID;

/**
 * Shared helpers that spin up a throwaway database and API server for one test.
 */
public final class TestSupport implements AutoCloseable {

	private final TestDatabaseScope database;
	private final StubOAuthProvider oauth;
	private final ServerScope serverScope;
	private final JettyServer jetty;
	private final HttpClient httpClient;
	private final ObjectMapper objectMapper;
	private final Path allureDir;

	public TestSupport() throws Exception {
		Properties properties = new Properties();
		try (InputStream in = TestSupport.class.getResourceAsStream("/application-test.properties")) {
			properties.load(in);
		}
		properties.setProperty("ENABLE_BOOM_ENDPOINT", "true");
		this.allureDir = Files.createTempDirectory("highspring-allure");
		Files.writeString(allureDir.resolve("index.html"), "<html><body>Allure Test Report</body></html>");
		properties.setProperty("ALLURE_REPORT_DIR", allureDir.toAbsolutePath().toString());

		AppConfiguration configuration = new AppConfiguration(properties);
		this.database = new TestDatabaseScope(configuration);
		this.oauth = new StubOAuthProvider();
		this.serverScope = new ServerScope(configuration, database, oauth);
		this.jetty = new JettyServer(serverScope, "127.0.0.1", 0);
		this.httpClient = new HttpClient();
		this.httpClient.start();
		this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
	}

	public String baseUri() {
		return jetty.getBaseUri();
	}

	public StubOAuthProvider oauth() {
		return oauth;
	}

	public ObjectMapper mapper() {
		return objectMapper;
	}

	public TestDatabaseScope database() {
		return database;
	}

	public String loginAs(String email, String roleHint) throws Exception {
		String code = UUID.randomUUID().toString();
		oauth.registerCode(code, new OAuthProvider.GoogleProfile("sub-" + email, email, "Test User"));
		String body = """
			{"code":"%s","redirectUri":"http://localhost:3000/auth/callback"}
			""".formatted(code);
		ContentResponse response = httpClient.newRequest(baseUri() + "/v1/auth/google/callback/")
			.method(HttpMethod.POST)
			.headers(h -> {
				h.put("Accept", "application/json");
				h.put("Content-Type", "application/json");
			})
			.body(new StringRequestContent("application/json", body))
			.send();
		if (response.getStatus() != 200) {
			throw new IllegalStateException("Login failed: " + response.getStatus() + " " + response.getContentAsString());
		}
		JsonNode json = objectMapper.readTree(response.getContentAsString());
		return json.get("sessionId").asText();
	}

	public ContentResponse request(HttpMethod method, String path, String sessionId, String jsonBody) throws Exception {
		var request = httpClient.newRequest(baseUri() + path).method(method);
		request.headers(h -> {
			h.put("Accept", "application/json");
			if (sessionId != null) {
				h.put("Authorization", "session:" + sessionId);
			}
			if (jsonBody != null) {
				h.put("Content-Type", "application/json");
			}
		});
		if (jsonBody != null) {
			request.body(new StringRequestContent("application/json", jsonBody));
		}
		return request.send();
	}

	@Override
	public void close() throws Exception {
		httpClient.stop();
		jetty.close();
		database.close();
	}
}
