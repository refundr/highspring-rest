package ca.refundr.highspring.api;

import ca.refundr.highspring.api.jetty.JettyServer;
import ca.refundr.highspring.api.scope.ServerScope;
import ca.refundr.highspring.common.config.AppConfiguration;
import ca.refundr.highspring.database.scope.MainDatabaseScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Process entry point — run this class to start the shopping cart API.
 *
 * <h2>Startup sequence</h2>
 *
 * <ol>
 *   <li>Load {@code application.properties} (copy from the template if missing).</li>
 *   <li>Open Postgres via {@link MainDatabaseScope} (Flyway migrates schema on boot).</li>
 *   <li>Build {@link ServerScope} (OAuth, mail, error reporters, pricing).</li>
 *   <li>Start embedded Jetty; {@link ca.refundr.highspring.api.jetty.RequestFilter} handles HTTP.</li>
 *   <li>Block the main thread until the JVM is stopped.</li>
 * </ol>
 *
 * <p>Default listen address is {@code http://127.0.0.1:8090} (8090 avoids colliding with Aragorn on 8080).
 *
 * <p>For how URLs map to Java classes, read {@link ca.refundr.highspring.api.resource.package-info}.
 */
public final class Server {

	private static final Logger log = LoggerFactory.getLogger(Server.class);

	private Server() {
	}

	public static void main(String[] args) throws Exception {

		// Support running from the repo root, the api/ module, or a packaged working directory.
		Path configPath = Path.of("api/src/main/resources/application.properties");
		if (!Files.exists(configPath)) {
			configPath = Path.of("src/main/resources/application.properties");
		}
		if (!Files.exists(configPath)) {
			configPath = Path.of("application.properties");
		}
		if (!Files.exists(configPath)) {
			throw new IllegalStateException(
				"Missing application.properties. Copy application.template.properties and fill in values."
			);
		}

		AppConfiguration configuration = new AppConfiguration(configPath);
		try (MainDatabaseScope database = new MainDatabaseScope(configuration);
		     ServerScope serverScope = new ServerScope(configuration, database);
		     JettyServer jetty = new JettyServer(
			     serverScope,
			     configuration.getString("SERVER_HOST", "0.0.0.0"),
			     configuration.getInt("SERVER_PORT", 8090)
		     )) {
			log.info("Highspring API listening on {}", jetty.getBaseUri());
			// Keep the process alive; Jetty serves requests on background threads.
			Thread.currentThread().join();
		}
	}
}
