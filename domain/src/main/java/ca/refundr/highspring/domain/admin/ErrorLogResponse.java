package ca.refundr.highspring.domain.admin;

import java.time.Instant;
import java.util.UUID;

/**
 * One serious server error saved for admins to review.
 *
 * @param id             row id in {@code api_error_log}
 * @param level          log level string (typically {@code ERROR})
 * @param loggerName     logger / source name stored with the event
 * @param message        short failure message
 * @param stackTrace     full stack trace text (may be long)
 * @param requestMethod  HTTP method of the failing request, if known
 * @param requestPath    request URI (may include query string) of the failing call
 * @param userId         signed-in user when the error happened; {@code null} if anonymous
 * @param createdAt      when the error was persisted
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
