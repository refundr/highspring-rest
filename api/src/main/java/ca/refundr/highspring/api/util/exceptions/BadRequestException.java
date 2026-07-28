package ca.refundr.highspring.api.util.exceptions;

/**
 * Expected bad client input (HTTP 400). Not a server crash — do not save to api_error_log.
 */
public final class BadRequestException extends RuntimeException {

	public BadRequestException(String message) {
		super(message);
	}

	public BadRequestException(String message, Throwable cause) {
		super(message, cause);
	}
}
