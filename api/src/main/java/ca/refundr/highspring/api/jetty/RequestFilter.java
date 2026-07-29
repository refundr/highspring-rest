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
 * Servlet filter that is the <strong>front door</strong> for every API request.
 *
 * <h2>What happens on each call (read this first)</h2>
 *
 * <ol>
 *   <li>Apply CORS headers so the Remix app on another origin can call us.</li>
 *   <li>Open a {@link RequestScope} (DB access, JSON helpers, etc. for this one request).</li>
 *   <li>Turn the URI into a path like {@code v1/products/} (no leading slash).</li>
 *   <li>Build a {@link RootResource} and call {@code getByPath(path)} to find the handler
 *       (see {@link ca.refundr.highspring.api.resource.package-info}).</li>
 *   <li>Run {@code matching.processRequest()} → {@code httpGet}/{@code httpPost}/…</li>
 *   <li>Write the {@link ServerResponse} to the servlet output.</li>
 * </ol>
 *
 * <h2>Exception policy (important for production)</h2>
 *
 * <ul>
 *   <li>{@link RequestFailedException} — expected 4xx (auth, forbidden). Answer only; do not log to DB.</li>
 *   <li>{@link PurchasesResource.ProductNotFound} — 404 for unknown catalog ids.</li>
 *   <li>{@link BadRequestException} — 400 for bad client input / empty cart / bad JSON.</li>
 *   <li>Any other {@link Exception} — 500, full stack saved to {@code api_error_log}, email developer.</li>
 * </ul>
 *
 * <p>Jetty is configured so this filter runs for all dispatcher types; otherwise requests can
 * mysteriously 404 before reaching our code.
 *
 * @see RootResource
 * @see ca.refundr.highspring.api.resource.version1.Version1Resource
 */
public final class RequestFilter implements Filter {

	private static final Logger log = LoggerFactory.getLogger(RequestFilter.class);
	private final ServerScope serverScope;

	public RequestFilter(ServerScope serverScope) {
		this.serverScope = Preconditions.checkNotNull(serverScope, "serverScope");
	}

	/** Registered as {@code /*} so every path hits this filter. */
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

	/**
	 * Route + execute one request. Throws expected client exceptions up to {@link #doFilter}.
	 */
	private void handleRequest(HttpServletRequest request, HttpServletResponse response, FilterChain chain,
		RequestScope requestScope) throws IOException, ServletException {
		String path = extractResourcePath(request);

		// Fresh root for every request — resources are cheap and hold no cross-request state.
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
			// Auth helpers throw this from inside httpGet/httpPost — still expected 4xx.
			e.getServerResponse().send(requestScope.getResponseWriter());
		}
	}

	/**
	 * Turns the servlet request into a path relative to the API root (no leading slash).
	 * Example: {@code http://host:8090/v1/cart/} → {@code v1/cart/}.
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

	/**
	 * Persist + email the failure, then send a generic 500 body to the client.
	 * Reporter failures must never replace the original error response.
	 */
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

	/** Allow the Remix storefront (and any configured origins) to call this API from the browser. */
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
					response.setHeader("Vary", "Origin");
					break;
				}
			}
		}
	}

	@Override
	public void destroy() {
	}
}
