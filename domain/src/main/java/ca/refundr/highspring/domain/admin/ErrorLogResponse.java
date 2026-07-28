package ca.refundr.highspring.domain.admin;

import java.time.Instant;
import java.util.UUID;

/**
 * One serious server error saved for admins to review.
 */
public record ErrorLogResponse(
	long id,
	String level,
	String loggerName,
	String message,
	String stackTrace,
	String requestMethod,
	String requestPath,
	UUID userId,
	Instant createdAt
) {
}
