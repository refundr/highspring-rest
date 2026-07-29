package ca.refundr.highspring.domain.auth;

/**
 * Body for the gated E2E-only login endpoint ({@code POST /v1/auth/e2e/login/}).
 *
 * @param email        shopper email to upsert (also used to derive a fake Google subject)
 * @param displayName  optional display name; defaults on the server if blank
 */
public record E2eLoginRequest(String email, String displayName) {
}
