package ca.refundr.highspring.api.resource;

import ca.refundr.highspring.api.resource.ping.PingResource;
import ca.refundr.highspring.api.resource.version1.Version1Resource;
import ca.refundr.highspring.api.scope.RequestScope;
import ca.refundr.highspring.api.util.ServerResponse;

import java.util.List;

import static org.eclipse.jetty.http.HttpStatus.OK_200;

/**
 * Root of the HTTP resource tree — the first object {@code RequestFilter} creates for each call.
 *
 * <h2>Why this class exists</h2>
 *
 * <p>Think of URLs as a folder tree. {@code RootResource} is the “/” folder. It does not implement
 * shopping features itself; it either:
 * <ul>
 *   <li>Answers {@code GET /} with a tiny JSON hello message, or</li>
 *   <li>Hands {@code /ping} to {@link PingResource} (liveness, no auth), or</li>
 *   <li>Hands longer paths to {@link Version1Resource} under {@code /v1/}.</li>
 * </ul>
 *
 * <h2>How routing starts</h2>
 *
 * <p>{@link ca.refundr.highspring.api.jetty.RequestFilter} does roughly:
 * <pre>
 *   String path = "v1/cart/";          // from the request URI, no leading slash
 *   RootResource root = new RootResource(requestScope);
 *   AbstractResource match = root.getByPath(path);
 *   match.processRequest();            // calls httpGet / httpPost / …
 * </pre>
 *
 * <p>{@link #getByPath(String)} (inherited) checks this node’s {@link #getRelativePath()},
 * then asks {@link #getDescendantByPath(String)} for children. Children today are
 * {@link PingResource} and {@link Version1Resource}.
 *
 * <h2>Adding a new API version later</h2>
 *
 * <p>You would add another child next to {@code Version1Resource}, e.g. {@code Version2Resource}
 * with {@code getRelativePath() = "v2/"}. Old clients keep calling {@code /v1/...}.
 *
 * @see Version1Resource
 * @see AbstractResource
 * @see ca.refundr.highspring.api.jetty.RequestFilter
 */
public final class RootResource extends AbstractResource {

	public RootResource(RequestScope scope) {
		super(scope);
	}

	/**
	 * Empty string means “this node is the API root” (matches {@code GET /}).
	 */
	@Override
	public String getRelativePath() {
		return "";
	}

	/**
	 * Health/hello for {@code GET /}. Useful to confirm the server is up without auth.
	 */
	@Override
	public ServerResponse httpGet() {
		return writer -> writer.sendJson(OK_200, java.util.Map.of(
			"name", "highspring",
			"message", "Shopping cart API"
		));
	}

	/**
	 * Children of the root: liveness ({@link PingResource}) and version 1 of the API.
	 *
	 * @param relativePath path left after the root segment (for root, that is the full path)
	 */
	@Override
	protected <T extends AbstractResource> T getDescendantByPath(String relativePath) {
		// Suppliers create children lazily — only the matching branch is constructed.
		return getDescendantFromChildren(relativePath, List.of(
			() -> new PingResource(scope, this),
			() -> new Version1Resource(scope, this)
		));
	}
}
