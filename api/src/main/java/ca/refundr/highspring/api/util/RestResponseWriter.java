package ca.refundr.highspring.api.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Low-level helper that writes status, headers, and body to the HTTP response.
 */
public final class RestResponseWriter {

	private final HttpServletResponse response;
	private final ObjectMapper objectMapper;

	public RestResponseWriter(HttpServletResponse response, ObjectMapper objectMapper) {
		this.response = Preconditions.checkNotNull(response, "response");
		this.objectMapper = Preconditions.checkNotNull(objectMapper, "objectMapper");
	}

	public void sendEmpty(int status) throws IOException {
		sendEmpty(status, Map.of());
	}

	public void sendEmpty(int status, Map<String, String> headers) throws IOException {
		applyHeaders(headers);
		response.setStatus(status);
		response.setContentLength(0);
	}

	public void sendText(int status, String body) throws IOException {
		sendText(status, Map.of(), body);
	}

	public void sendText(int status, Map<String, String> headers, String body) throws IOException {
		applyHeaders(headers);
		response.setStatus(status);
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		response.setContentType("text/plain; charset=utf-8");
		byte[] bytes = body == null ? new byte[0] : body.getBytes(StandardCharsets.UTF_8);
		response.setContentLength(bytes.length);
		response.getOutputStream().write(bytes);
	}

	public void sendJson(int status, Object body) throws IOException {
		sendJson(status, Map.of("Content-Type", "application/json; charset=utf-8"), body);
	}

	public void sendJson(int status, Map<String, String> headers, Object body) throws IOException {
		applyHeaders(headers);
		response.setStatus(status);
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		if (!headers.containsKey("Content-Type")) {
			response.setContentType("application/json; charset=utf-8");
		}
		byte[] bytes = objectMapper.writeValueAsBytes(body);
		response.setContentLength(bytes.length);
		response.getOutputStream().write(bytes);
	}

	public void sendBytes(int status, String contentType, byte[] body) throws IOException {
		response.setStatus(status);
		response.setContentType(contentType);
		byte[] bytes = body == null ? new byte[0] : body;
		response.setContentLength(bytes.length);
		response.getOutputStream().write(bytes);
	}

	private void applyHeaders(Map<String, String> headers) {
		if (headers == null) {
			return;
		}
		headers.forEach(response::setHeader);
	}
}
