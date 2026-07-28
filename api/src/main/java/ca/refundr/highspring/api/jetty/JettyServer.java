package ca.refundr.highspring.api.jetty;

import ca.refundr.highspring.api.scope.ServerScope;
import com.google.common.base.Preconditions;
import jakarta.servlet.DispatcherType;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;

import java.net.InetSocketAddress;
import java.util.EnumSet;

/**
 * Starts the embedded Jetty web server that hosts the shopping cart API.
 */
public final class JettyServer implements AutoCloseable {

	private final Server server;
	private final ServerScope serverScope;

	public JettyServer(ServerScope serverScope, String host, int port) throws Exception {
		this.serverScope = Preconditions.checkNotNull(serverScope, "serverScope");
		this.server = new Server(new InetSocketAddress(host, port));

		ServletContextHandler context = new ServletContextHandler();
		context.setContextPath("/");
		RequestFilter filter = new RequestFilter(serverScope);
		// Dispatcher types must be set — null means the filter never runs and Jetty returns 404.
		context.addFilter(new FilterHolder(filter), filter.getPathSpec(), EnumSet.allOf(DispatcherType.class));
		server.setHandler(context);
		server.start();
	}

	public int getPort() {
		return ((ServerConnector) server.getConnectors()[0]).getLocalPort();
	}

	public String getBaseUri() {
		return "http://127.0.0.1:" + getPort();
	}

	@Override
	public void close() throws Exception {
		server.stop();
		serverScope.close();
	}
}
