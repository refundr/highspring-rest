package ca.refundr.highspring.domain.auth;

/**
 * Asks the server for a Google sign-in URL.
 */
public record GoogleAuthUrlRequest(String redirectUri, String state) {
}
