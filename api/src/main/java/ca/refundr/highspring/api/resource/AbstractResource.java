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
 * One URL path the API understands. Child classes say which HTTP methods they support
 * and what to do for each.
 */
public abstract class AbstractResource {

	public static final String ERROR_500_MESSAGE =
		"Something went wrong on our side. Please try again in a moment.";

	protected final RequestScope scope;
	protected final StringJoiner supportedMethods = new StringJoiner(", ");

	protected AbstractResource(RequestScope scope) {
		this.scope = Preconditions.checkNotNull(scope, "scope");
		supportedMethods.add("OPTIONS");
	}

	public abstract String getRelativePath();

	protected abstract <T extends AbstractResource> T getDescendantByPath(String relativePath);

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

	public <T extends AbstractResource> T getByPath(String pathRelativeToResource) {
		String pathSegment = getRelativePath();
		if (!pathRelativeToResource.startsWith(pathSegment)) {
			return null;
		}
		boolean matchFound;
		if (pathSegment.isEmpty() && pathRelativeToResource.isEmpty()) {
			AbstractResource descendant = getDescendantByPath(pathRelativeToResource);
			if (descendant != null) {
				@SuppressWarnings("unchecked")
				T match = (T) descendant;
				return match;
			}
			matchFound = true;
		} else {
			matchFound = pathRelativeToResource.length() == pathSegment.length();
		}
		if (matchFound) {
			@SuppressWarnings("unchecked")
			T match = (T) this;
			return match;
		}
		return getDescendantByPath(pathRelativeToResource.substring(pathSegment.length()));
	}

	protected static String getNextSegment(String relativePath, boolean includeTrailingSlash) {
		int endOfSegment = relativePath.indexOf('/');
		if (endOfSegment == -1) {
			endOfSegment = relativePath.length();
		} else if (includeTrailingSlash) {
			++endOfSegment;
		}
		return relativePath.substring(0, endOfSegment);
	}

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
