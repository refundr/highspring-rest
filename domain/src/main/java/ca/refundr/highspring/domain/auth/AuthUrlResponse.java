package ca.refundr.highspring.domain.auth;

/**
 * Google consent URL for the browser to open.
 *
 * @param uri  full https://accounts.google.com/... authorization URL
 */
public record AuthUrlResponse(String uri) {
}
