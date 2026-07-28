package ca.refundr.highspring.api.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.util.Collections;

/**
 * Convenience wrapper around the incoming HTTP request (path, headers, JSON body).
 */
public final class RestRequest {

	private final HttpServletRequest servletRequest;
	private final ObjectMapper objectMapper;

	public RestRequest(HttpServletRequest servletRequest, ObjectMapper objectMapper) {
		this.servletRequest = Preconditions.checkNotNull(servletRequest, "servletRequest");
		this.objectMapper = Preconditions.checkNotNull(objectMapper, "objectMapper");
	}

	public HttpServletRequest getServletRequest() {
		return servletRequest;
	}

	public String getMethod() {
		return servletRequest.getMethod();
	}

	public String getPathInfo() {
		String path = servletRequest.getPathInfo();
		return path == null ? "/" : path;
	}

	public String getHeader(String name) {
		return servletRequest.getHeader(name);
	}

	public boolean acceptsJson() {
		String accept = servletRequest.getHeader("Accept");
		return accept == null || accept.contains("*/*") || accept.contains("application/json");
	}

	public <T> T getBody(Class<T> type) throws IOException {
		if (servletRequest.getContentLengthLong() == 0) {
			return null;
		}
		return objectMapper.readValue(servletRequest.getInputStream(), type);
	}

	public String getQueryParameter(String name) {
		return servletRequest.getParameter(name);
	}

	@Override
	public String toString() {
		return getMethod() + " " + getPathInfo() + " headers=" + Collections.list(servletRequest.getHeaderNames());
	}
}
