package ca.refundr.highspring.api.util.exceptions;

import ca.refundr.highspring.api.util.ServerResponse;
import com.google.common.base.Preconditions;

/**
 * Stops normal request handling and sends a specific HTTP answer instead
 * (for example 401 or 404). This is an expected failure, not a crash.
 */
public final class RequestFailedException extends RuntimeException {

	private final ServerResponse serverResponse;

	public RequestFailedException(ServerResponse serverResponse) {
		this.serverResponse = Preconditions.checkNotNull(serverResponse, "serverResponse");
	}

	public ServerResponse getServerResponse() {
		return serverResponse;
	}
}
