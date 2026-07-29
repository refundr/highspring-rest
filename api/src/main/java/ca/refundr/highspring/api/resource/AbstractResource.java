package ca.refundr.highspring.api.resource;

import ca.refundr.highspring.api.scope.RequestScope;
import ca.refundr.highspring.api.util.ServerResponse;
import ca.refundr.highspring.api.util.exceptions.RequestFailedException;
import ca.refundr.highspring.database.model.UserRole;
import ca.refundr.highspring.database.row.ApiSessionRow;
import ca.refundr.highspring.database.row.AppUserRow;
import com.google.common.base.Preconditions;
import org.eclipse.jetty.http.HttpMethod;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;

import java.io.IOException;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.eclipse.jetty.http.HttpStatus.FORBIDDEN_403;
import static org.eclipse.jetty.http.HttpStatus.METHOD_NOT_ALLOWED_405;
import static org.eclipse.jetty.http.HttpStatus.NO_CONTENT_204;
import static org.eclipse.jetty.http.HttpStatus.UNAUTHORIZED_401;

/**
 * Base class for one URL node in the hand-rolled resource tree.
 *
 * <h2>Key methods</h2>
 *
 * <ul>
 *   <li>{@link #getRelativePath()} — the path segment this class owns (usually ends with {@code /}).</li>
 *   <li>{@link #getDescendantByPath(String)} — “who are my children?” for deeper paths.</li>
 *   <li>{@link #httpGet()} / {@link #httpPost()} / … — what happens for that HTTP verb.</li>
 *   <li>{@link #processRequest()} — picks the verb method (called by the filter after routing).</li>
 *   <li>{@link #requireSessionUser()} / {@link #requireAdmin()} — auth helpers used by protected endpoints.</li>
 * </ul>
 *
 * <h2>{@code getByPath} algorithm (the important bit)</h2>
 *
 * <p>Given {@code pathRelativeToResource} like {@code "v1/cart/"}:
 * <ol>
 *   <li>If the path does not start with this node’s {@code getRelativePath()}, return {@code null}
 *       (this branch is wrong).</li>
 *   <li>If the path equals this node’s segment exactly, return <em>this</em> (we are the leaf).</li>
 *   <li>Otherwise strip our segment and ask {@link #getDescendantByPath(String)} to find a child.</li>
 * </ol>
 *
 * <p>Parents usually override {@code getDescendantByPath} with
 * {@link #getDescendantFromChildren(String, java.util.List)} or the UUID/long helpers.
 *
 * @see package-info
 * @see RootResource
 * @see AbstractChildResource
 */
public abstract class AbstractResource {

	/** Safe message returned to browsers on unexpected 500s (never leak internal details). */
	public static final String ERROR_500_MESSAGE =
		"Something went wrong on our side. Please try again in a moment.";

	/** Per-request services (DB, JSON, OAuth, current user after auth). */
	protected final RequestScope scope;

	/**
	 * HTTP verbs this resource allows. Always includes OPTIONS.
	 * Subclasses should {@code supportedMethods.add("GET")} etc. in their constructor.
	 */
	protected final StringJoiner supportedMethods = new StringJoiner(", ");

	protected AbstractResource(RequestScope scope) {
		this.scope = Preconditions.checkNotNull(scope, "scope");
		supportedMethods.add("OPTIONS");
	}

	/**
	 * Path segment owned by this node, relative to its parent.
	 * Examples: {@code ""} (root), {@code "v1/"}, {@code "products/"}.
	 */
	public abstract String getRelativePath();

	/**
	 * Resolve a child for the remainder of the path after this node’s segment.
	 * Return {@code null} if nothing matches (caller will 404).
	 */
	protected abstract <T extends AbstractResource> T getDescendantByPath(String relativePath);

	/**
	 * Dispatch to {@code httpGet}/{@code httpPost}/… based on the request method.
	 * Resources that do not override a verb inherit “405 Method Not Allowed”.
	 */
	public final ServerResponse processRequest() throws IOException {
		return switch (HttpMethod.valueOf(scope.getRequest().getMethod())) {
			case OPTIONS -> httpOptions();
			case POST -> httpPost();
			case GET -> httpGet();
			case PUT -> httpPut();
			case DELETE -> httpDelete();
			default -> methodNotAllowed();
		};
	}

	public ServerResponse httpOptions() {
		return writer -> writer.sendEmpty(NO_CONTENT_204, java.util.Map.of("Allow", supportedMethods.toString()));
	}

	public ServerResponse httpPost() throws IOException {
		return methodNotAllowed();
	}

	public ServerResponse httpGet() throws IOException {
		return methodNotAllowed();
	}

	public ServerResponse httpPut() throws IOException {
		return methodNotAllowed();
	}

	public ServerResponse httpDelete() throws IOException {
		return methodNotAllowed();
	}

	private ServerResponse methodNotAllowed() {
		return writer -> writer.sendEmpty(METHOD_NOT_ALLOWED_405, java.util.Map.of("Allow", supportedMethods.toString()));
	}

	/**
	 * Walk from this node down the tree until a resource matches {@code pathRelativeToResource}.
	 *
	 * @param pathRelativeToResource full remaining path from this node’s perspective
	 *                               (at the root, that is the whole request path without a leading slash)
	 * @return the matching resource, or {@code null} if no branch fits
	 */
	public <T extends AbstractResource> T getByPath(String pathRelativeToResource) {
		String pathSegment = getRelativePath();
		if (!pathRelativeToResource.startsWith(pathSegment)) {
			return null;
		}
		boolean matchFound;
		if (pathSegment.isEmpty() && pathRelativeToResource.isEmpty()) {
			// Special case: GET / on the empty root path.
			AbstractResource descendant = getDescendantByPath(pathRelativeToResource);
			if (descendant != null) {
				@SuppressWarnings("unchecked")
				T match = (T) descendant;
				return match;
			}
			matchFound = true;
		} else {
			// Exact match means this node is the target (e.g. path "products/" on ProductsResource).
			matchFound = pathRelativeToResource.length() == pathSegment.length();
		}
		if (matchFound) {
			@SuppressWarnings("unchecked")
			T match = (T) this;
			return match;
		}
		// Deeper path: strip our segment and ask children about the rest.
		return getDescendantByPath(pathRelativeToResource.substring(pathSegment.length()));
	}

	/**
	 * First path segment, optionally keeping the trailing slash.
	 * Example: {@code "aaaaaaaa-…/"} → {@code "aaaaaaaa-…"} or {@code "aaaaaaaa-…/"}.
	 */
	protected static String getNextSegment(String relativePath, boolean includeTrailingSlash) {
		int endOfSegment = relativePath.indexOf('/');
		if (endOfSegment == -1) {
			endOfSegment = relativePath.length();
		} else if (includeTrailingSlash) {
			++endOfSegment;
		}
		return relativePath.substring(0, endOfSegment);
	}

	/**
	 * Try each child supplier’s {@link #getByPath(String)} until one returns non-null.
	 * This is how parents register route tables without a framework router.
	 */
	protected <T extends AbstractResource> T getDescendantFromChildren(String relativePath,
		java.util.List<Supplier<? extends AbstractResource>> children) {
		for (Supplier<? extends AbstractResource> child : children) {
			T match = child.get().getByPath(relativePath);
			if (match != null) {
				return match;
			}
		}
		return null;
	}

	/**
	 * Match {@code /…/{uuid}/…} children. Invalid UUID text means “no match” (try another route).
	 */
	protected <T extends AbstractResource> T getDescendantFromChildByUuid(String relativePath,
		Function<UUID, ? extends AbstractResource> childById) {
		try {
			String idAsString = getNextSegment(relativePath, false);
			UUID id = UUID.fromString(idAsString);
			return childById.apply(id).getByPath(relativePath);
		} catch (IllegalArgumentException ignored) {
			return null;
		}
	}

	/**
	 * Match {@code /…/{longId}/…} children (used for {@code api_error_log} ids).
	 */
	protected <T extends AbstractResource> T getDescendantFromChildByLong(String relativePath,
		Function<Long, ? extends AbstractResource> childById) {
		try {
			String idAsString = getNextSegment(relativePath, false);
			long id = Long.parseLong(idAsString);
			return childById.apply(id).getByPath(relativePath);
		} catch (NumberFormatException ignored) {
			return null;
		}
	}

	/**
	 * Require {@code Authorization: session:{uuid}} and load the user.
	 * Also refreshes session activity time.
	 *
	 * @throws RequestFailedException with HTTP 401 if missing/invalid/expired
	 */
	protected AppUserRow requireSessionUser() {
		String authorization = scope.getRequest().getHeader("Authorization");
		if (authorization == null) {
			throw unauthorized();
		}
		Pattern pattern = Pattern.compile("^session:\\s*(.+)$", Pattern.CASE_INSENSITIVE);
		Matcher matcher = pattern.matcher(authorization);
		if (!matcher.find()) {
			throw unauthorized();
		}
		UUID sessionId;
		try {
			sessionId = UUID.fromString(matcher.group(1).trim());
		} catch (IllegalArgumentException e) {
			throw unauthorized();
		}

		return scope.getDatabase().transactionWithResult(connection -> {
			DSLContext dsl = DSL.using(connection);
			ApiSessionRow session = ApiSessionRow.fetchActive(dsl, sessionId);
			if (session == null) {
				throw unauthorized();
			}
			AppUserRow user = AppUserRow.fetchById(dsl, session.getUserId());
			if (user == null) {
				throw unauthorized();
			}
			ApiSessionRow.touch(connection, sessionId);
			scope.setCurrentUser(user);
			scope.setCurrentSessionId(sessionId);
			return user;
		});
	}

	/**
	 * Same as {@link #requireSessionUser()} plus role must be {@link UserRole#ADMIN}.
	 *
	 * @throws RequestFailedException with HTTP 403 if the user is only a customer
	 */
	protected AppUserRow requireAdmin() {
		AppUserRow user = requireSessionUser();
		if (user.getRole() != UserRole.ADMIN) {
			throw forbidden();
		}
		return user;
	}

	protected RequestFailedException unauthorized() {
		return new RequestFailedException(writer -> writer.sendText(
			UNAUTHORIZED_401,
			java.util.Map.of("WWW-Authenticate", "session"),
			"Authorization is required."
		));
	}

	protected RequestFailedException forbidden() {
		return new RequestFailedException(writer -> writer.sendText(FORBIDDEN_403, "You do not have permission."));
	}
}
