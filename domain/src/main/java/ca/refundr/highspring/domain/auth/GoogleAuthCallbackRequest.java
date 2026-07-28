package ca.refundr.highspring.domain.auth;

/**
 * Completes Google login after the user returns with an authorization code.
 */
public record GoogleAuthCallbackRequest(String code, String redirectUri) {
}
