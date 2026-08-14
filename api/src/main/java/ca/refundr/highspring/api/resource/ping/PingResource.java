package ca.refundr.highspring.api.resource.ping;

import ca.refundr.highspring.api.resource.AbstractChildResource;
import ca.refundr.highspring.api.resource.AbstractResource;
import ca.refundr.highspring.api.resource.RootResource;
import ca.refundr.highspring.api.scope.RequestScope;
import ca.refundr.highspring.api.util.ServerResponse;
import org.eclipse.jetty.http.HttpMethod;

import java.util.Map;

import static org.eclipse.jetty.http.HttpStatus.OK_200;

/**
 * Liveness probe: {@code GET /ping} (also accepts {@code GET /ping/}).
 *
 * <p>No authentication. Load balancers and operators can call this to confirm the
 * process is up without hitting the database or requiring a session.
 */
public final class PingResource extends AbstractChildResource<RootResource> {

	public PingResource(RequestScope scope, RootResource parent) {
		super(scope, parent);
		supportedMethods.add(HttpMethod.GET.asString());
	}

	/**
	 * No trailing slash so {@code GET /ping} matches (health checks rarely add one).
	 * {@link #getDescendantByPath(String)} still accepts {@code /ping/}.
	 */
	@Override
	public String getRelativePath() {
		return "ping";
	}

	@Override
	public ServerResponse httpGet() {
		return writer -> writer.sendJson(OK_200, Map.of("status", "ok"));
	}

	@Override
	protected <T extends AbstractResource> T getDescendantByPath(String relativePath) {
		if ("/".equals(relativePath)) {
			@SuppressWarnings("unchecked")
			T self = (T) this;
			return self;
		}
		return null;
	}
}
