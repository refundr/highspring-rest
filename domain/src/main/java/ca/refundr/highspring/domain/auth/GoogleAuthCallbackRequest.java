package ca.refundr.highspring.domain.auth;

/**
 * Body for exchanging Google's authorization code for a Highspring session.
 *
 * @param code         one-time code from Google's redirect query string
 * @param redirectUri  same redirect URI used when the consent URL was built
 */
public record GoogleAuthCallbackRequest(String code, String redirectUri) {
}
