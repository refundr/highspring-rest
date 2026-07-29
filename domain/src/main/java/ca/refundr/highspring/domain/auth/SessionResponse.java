package ca.refundr.highspring.domain.auth;

import java.util.UUID;

/**
 * A logged-in session the client stores and sends on later requests.
 *
 * @param sessionId    API session id — send as {@code Authorization: session:{uuid}}
 * @param userId       stable Highspring user id
 * @param email        Google account email
 * @param displayName  optional profile name from Google; may be {@code null}
 * @param role         {@code CUSTOMER} or {@code ADMIN}
 */
public record SessionResponse(UUID sessionId, UUID userId, String email, String displayName, String role) {
}
