package ca.refundr.highspring.domain.auth;

import java.util.UUID;

/**
 * A logged-in session the client stores and sends on later requests.
 */
public record SessionResponse(UUID sessionId, UUID userId, String email, String displayName, String role) {
}
