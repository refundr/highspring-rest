package ca.refundr.highspring.domain.auth;

/**
 * Body for building the Google OAuth consent URL.
 *
 * @param redirectUri  where Google should send the browser after consent (must match Google Cloud config)
 * @param state        opaque value echoed back by Google (CSRF / correlation); may be empty
 */
public record GoogleAuthUrlRequest(String redirectUri, String state) {
}
