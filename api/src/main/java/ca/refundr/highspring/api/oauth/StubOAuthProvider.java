package ca.refundr.highspring.api.oauth;

import com.google.common.base.Preconditions;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fake Google login for automated tests — no network calls.
 */
public final class StubOAuthProvider implements OAuthProvider {

	private final Map<String, GoogleProfile> codes = new ConcurrentHashMap<>();

	public void registerCode(String code, GoogleProfile profile) {
		Preconditions.checkNotNull(code, "code");
		Preconditions.checkNotNull(profile, "profile");
		codes.put(code, profile);
	}

	@Override
	public String buildAuthorizationUrl(String redirectUri, String state) {
		return "https://example.test/oauth?redirect_uri=" + redirectUri + "&state=" + state;
	}

	@Override
	public GoogleProfile exchangeCode(String code, String redirectUri) {
		GoogleProfile profile = codes.get(code);
		if (profile == null) {
			throw new IllegalArgumentException("Unknown auth code");
		}
		return profile;
	}
}
