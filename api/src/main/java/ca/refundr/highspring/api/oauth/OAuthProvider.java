package ca.refundr.highspring.api.oauth;

/**
 * Talks to an identity provider (Google today) to finish a login.
 * Tests can swap in a fake implementation so we do not call Google.
 */
public interface OAuthProvider {

	String buildAuthorizationUrl(String redirectUri, String state);

	GoogleProfile exchangeCode(String code, String redirectUri);

	/**
	 * Who Google says this person is.
	 */
	record GoogleProfile(String subject, String email, String displayName) {
	}
}
