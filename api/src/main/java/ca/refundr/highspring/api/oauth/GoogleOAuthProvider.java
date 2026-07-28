package ca.refundr.highspring.api.oauth;

import ca.refundr.highspring.api.util.exceptions.BadRequestException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.util.FormRequestContent;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.util.Fields;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Real Google OAuth 2.0 sign-in: builds the Google URL and swaps the code for the user's profile.
 */
public final class GoogleOAuthProvider implements OAuthProvider {

	private static final String AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth";
	private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
	private static final String USERINFO_URL = "https://www.googleapis.com/oauth2/v3/userinfo";

	private final String clientId;
	private final String clientSecret;
	private final HttpClient httpClient;
	private final ObjectMapper objectMapper;

	public GoogleOAuthProvider(String clientId, String clientSecret, HttpClient httpClient, ObjectMapper objectMapper) {
		this.clientId = Preconditions.checkNotNull(clientId, "clientId");
		this.clientSecret = Preconditions.checkNotNull(clientSecret, "clientSecret");
		this.httpClient = Preconditions.checkNotNull(httpClient, "httpClient");
		this.objectMapper = Preconditions.checkNotNull(objectMapper, "objectMapper");
	}

	@Override
	public String buildAuthorizationUrl(String redirectUri, String state) {
		Preconditions.checkNotNull(redirectUri, "redirectUri");
		String safeState = state == null ? "" : state;
		return AUTH_URL
			+ "?client_id=" + enc(clientId)
			+ "&redirect_uri=" + enc(redirectUri)
			+ "&response_type=code"
			+ "&scope=" + enc("openid email profile")
			+ "&access_type=online"
			+ "&include_granted_scopes=true"
			+ "&state=" + enc(safeState);
	}

	@Override
	public GoogleProfile exchangeCode(String code, String redirectUri) {
		Preconditions.checkNotNull(code, "code");
		Preconditions.checkNotNull(redirectUri, "redirectUri");
		try {
			Fields fields = new Fields();
			fields.put("code", code);
			fields.put("client_id", clientId);
			fields.put("client_secret", clientSecret);
			fields.put("redirect_uri", redirectUri);
			fields.put("grant_type", "authorization_code");

			ContentResponse tokenResponse = httpClient.newRequest(TOKEN_URL)
				.method(HttpMethod.POST)
				.body(new FormRequestContent(fields))
				.send();
			int tokenStatus = tokenResponse.getStatus();
			if (tokenStatus >= 500) {
				throw new IllegalStateException("Google token exchange failed: " + tokenStatus);
			}
			if (tokenStatus >= 400) {
				throw new BadRequestException("Google token exchange failed: " + tokenStatus);
			}
			JsonNode tokenJson = objectMapper.readTree(tokenResponse.getContentAsString());
			String accessToken = tokenJson.path("access_token").asText(null);
			if (accessToken == null || accessToken.isBlank()) {
				throw new BadRequestException("Google did not return an access token");
			}

			ContentResponse userResponse = httpClient.newRequest(USERINFO_URL)
				.method(HttpMethod.GET)
				.headers(h -> h.put("Authorization", "Bearer " + accessToken))
				.send();
			int userStatus = userResponse.getStatus();
			if (userStatus >= 500) {
				throw new IllegalStateException("Google userinfo failed: " + userStatus);
			}
			if (userStatus >= 400) {
				throw new BadRequestException("Google userinfo failed: " + userStatus);
			}
			JsonNode user = objectMapper.readTree(userResponse.getContentAsString());
			String subject = user.path("sub").asText(null);
			String email = user.path("email").asText(null);
			String name = user.path("name").asText(email);
			if (subject == null || email == null) {
				throw new BadRequestException("Google profile missing sub/email");
			}

			return new GoogleProfile(subject, email, name);

		} catch (BadRequestException | IllegalStateException e) {
			throw e;
		} catch (Exception e) {
			throw new IllegalStateException("Google OAuth exchange failed", e);
		}
	}

	private static String enc(String value) {
		return URLEncoder.encode(value, StandardCharsets.UTF_8);
	}
}
