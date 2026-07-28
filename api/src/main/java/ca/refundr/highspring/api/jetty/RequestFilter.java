package ca.refundr.highspring.api.jetty;

import ca.refundr.highspring.api.resource.AbstractResource;
import ca.refundr.highspring.api.resource.RootResource;
import ca.refundr.highspring.api.resource.version1.purchase.PurchasesResource;
import ca.refundr.highspring.api.scope.RequestScope;
import ca.refundr.highspring.api.scope.ServerScope;
import ca.refundr.highspring.api.util.ServerResponse;
import ca.refundr.highspring.api.util.exceptions.BadRequestException;
import ca.refundr.highspring.api.util.exceptions.RequestFailedException;
import ca.refundr.highspring.common.error.ErrorReport;
import com.google.common.base.Preconditions;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static ca.refundr.highspring.api.resource.AbstractResource.ERROR_500_MESSAGE;
import static org.eclipse.jetty.http.HttpStatus.BAD_REQUEST_400;
import static org.eclipse.jetty.http.HttpStatus.INTERNAL_SERVER_ERROR_500;
import static org.eclipse.jetty.http.HttpStatus.METHOD_NOT_ALLOWED_405;
import static org.eclipse.jetty.http.HttpStatus.NOT_FOUND_404;

/**
 * Front door for every HTTP call: finds the matching URL handler, runs it,
 * and turns failures into the right status codes.
 * <p>
 * Expected client failures (4xx) are answered quietly.
 * Unexpected exceptions are answered as 500 and persisted (stack trace + email).
 */
public final class RequestFilter implements Filter {

	private static final Logger log = LoggerFactory.getLogger(RequestFilter.class);
	private final ServerScope serverScope;

	public RequestFilter(ServerScope serverScope) {
		this.serverScope = Preconditions.checkNotNull(serverScope, "serverScope");
	}

	public String getPathSpec() {
		return "/*";
	}

	@Override
	public void init(FilterConfig filterConfig) {
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
		throws IOException, ServletException {
		HttpServletRequest httpRequest = (HttpServletRequest) request;
		HttpServletResponse httpResponse = (HttpServletResponse) response;
		applyCors(httpRequest, httpResponse);
		if ("OPTIONS".equalsIgnoreCase(httpRequest.getMethod())) {
			httpResponse.setStatus(HttpServletResponse.SC_NO_CONTENT);
			return;
		}

		try (RequestScope requestScope = serverScope.createRequest(httpRequest, httpResponse)) {
			try {
				handleRequest(httpRequest, httpResponse, chain, requestScope);
			} catch (RequestFailedException e) {
				// Expected auth/permission/business 4xx — do not persist.
				e.getServerResponse().send(requestScope.getResponseWriter());
			} catch (PurchasesResource.ProductNotFound e) {
				requestScope.getResponseWriter().sendText(NOT_FOUND_404, e.getMessage());
			} catch (BadRequestException e) {
				requestScope.getResponseWriter().sendText(BAD_REQUEST_400, e.getMessage());
			} catch (Exception e) {
				// Unexpected: NullPointerException, SQLException wrappers, IllegalStateException, …
				reportAndRespond500(requestScope, httpRequest, e);
			}
		}
	}

	private void handleRequest(HttpServletRequest request, HttpServletResponse response, FilterChain chain,
		RequestScope requestScope) throws IOException, ServletException {
		String path = extractResourcePath(request);

		RootResource root = new RootResource(requestScope);
		AbstractResource matching = root.getByPath(path);
		if (matching == null) {
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);
			response.getWriter().write("Not found");
			return;
		}

		try {
			ServerResponse serverResponse;
			if ("TRACE".equalsIgnoreCase(request.getMethod())) {
				serverResponse = writer -> writer.sendEmpty(METHOD_NOT_ALLOWED_405);
			} else {
				serverResponse = matching.processRequest();
			}
			serverResponse.send(requestScope.getResponseWriter());
		} catch (RequestFailedException e) {
			e.getServerResponse().send(requestScope.getResponseWriter());
		}
	}

	/**
	 * Turns the servlet request into a path relative to the API root (no leading slash).
	 */
	static String extractResourcePath(HttpServletRequest request) {
		String uri = request.getRequestURI();
		String context = request.getContextPath();
		if (context != null && !context.isEmpty() && uri.startsWith(context)) {
			uri = uri.substring(context.length());
		}
		if (uri.startsWith("/")) {
			uri = uri.substring(1);
		}
		return uri;
	}

	private void reportAndRespond500(RequestScope requestScope, HttpServletRequest request, Exception e)
		throws IOException {
		log.error("Unhandled server error", e);
		ErrorReport report = new ErrorReport(
			e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage(),
			e,
			request.getMethod(),
			requestPath(request),
			requestScope.getCurrentUser() == null ? null : requestScope.getCurrentUser().getId()
		);
		try {
			requestScope.getErrorReporter().report(report);
		} catch (Exception reporterFailure) {
			log.error("ErrorReporter failed while handling a 500", reporterFailure);
		}
		requestScope.getResponseWriter().sendText(INTERNAL_SERVER_ERROR_500, ERROR_500_MESSAGE);
	}

	/**
	 * Prefer the full request URI so api_error_log rows show a usable path (pathInfo is often null).
	 */
	private static String requestPath(HttpServletRequest request) {
		String uri = request.getRequestURI();
		String query = request.getQueryString();
		if (query != null && !query.isBlank()) {
			return uri + "?" + query;
		}
		return uri == null || uri.isBlank() ? extractResourcePath(request) : uri;
	}

	private void applyCors(HttpServletRequest request, HttpServletResponse response) {
		String origin = request.getHeader("Origin");
		String allowed = serverScope.getConfiguration().getString("CORS_ORIGINS", "http://localhost:3000");
		if (origin != null) {
			for (String candidate : allowed.split(",")) {
				if (candidate.trim().equalsIgnoreCase(origin)) {
					response.setHeader("Access-Control-Allow-Origin", origin);
					response.setHeader("Access-Control-Allow-Credentials", "true");
					response.setHeader("Access-Control-Allow-Headers", "Authorization, Content-Type, Accept");
					response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
					break;
				}
			}
		}
	}

	@Override
	public void destroy() {
	}
}
