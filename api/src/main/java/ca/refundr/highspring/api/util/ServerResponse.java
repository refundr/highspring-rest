package ca.refundr.highspring.api.util;

import java.io.IOException;

/**
 * Something that knows how to write the HTTP answer back to the client.
 */
@FunctionalInterface
public interface ServerResponse {
	void send(RestResponseWriter writer) throws IOException;
}
