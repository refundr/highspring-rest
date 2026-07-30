package ca.refundr.highspring.api;

import ca.refundr.highspring.api.jetty.JettyServer;
import ca.refundr.highspring.api.scope.ServerScope;
import ca.refundr.highspring.common.config.AppConfiguration;
import ca.refundr.highspring.database.scope.MainDatabaseScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Process entry point — run this class to start the shopping cart API.
 *
 * <h2>Startup sequence</h2>
 *
 * <ol>
 *   <li>Load {@code application.properties} (filesystem, then classpath inside the jar).</li>
 *   <li>Open Postgres via {@link MainDatabaseScope} (Flyway migrates schema on boot).</li>
 *   <li>Build {@link ServerScope} (OAuth, mail, error reporters, pricing).</li>
 *   <li>Start embedded Jetty; {@link ca.refundr.highspring.api.jetty.RequestFilter} handles HTTP.</li>
 *   <li>Block the main thread until the JVM is stopped.</li>
 * </ol>
 *
 * <p>Default listen address is {@code http://127.0.0.1:8090} (8090 avoids colliding with Aragorn on 8080).
 * On PaaS (Render), {@code PORT} overrides {@code SERVER_PORT}.
 *
 * <p>For how URLs map to Java classes, read {@link ca.refundr.highspring.api.resource.package-info}.
 */
public final class Server {

	private static final Logger log = LoggerFactory.getLogger(Server.class);

	private Server() {
	}

	public static void main(String[] args) throws Exception {
		AppConfiguration configuration = loadConfiguration();
		int port = resolvePort(configuration);
		try (MainDatabaseScope database = new MainDatabaseScope(configuration);
		     ServerScope serverScope = new ServerScope(configuration, database);
		     JettyServer jetty = new JettyServer(
			     serverScope,
			     configuration.getString("SERVER_HOST", "0.0.0.0"),
			     port
		     )) {
			log.info("Highspring API listening on {}", jetty.getBaseUri());
			log.info("Admin Allure dir: {} (exists={})",
				serverScope.getAllureReportDir(),
				Files.isDirectory(serverScope.getAllureReportDir()));
			log.info("Admin JavaDoc dir: {} (exists={})",
				serverScope.getJavadocReportDir(),
				Files.isDirectory(serverScope.getJavadocReportDir()));
			Thread.currentThread().join();
		}
	}

	/** Filesystem first (local/IDE), then classpath (fat jar / Docker). */
	private static AppConfiguration loadConfiguration() throws IOException {
		Path configPath = Path.of("api/src/main/resources/application.properties");
		if (!Files.exists(configPath)) {
			configPath = Path.of("src/main/resources/application.properties");
		}
		if (!Files.exists(configPath)) {
			configPath = Path.of("application.properties");
		}
		if (Files.exists(configPath)) {
			log.info("Loading config from {}", configPath.toAbsolutePath());
			return new AppConfiguration(configPath);
		}

		Properties properties = new Properties();
		try (InputStream in = Server.class.getClassLoader().getResourceAsStream("application.properties")) {
			if (in == null) {
				try (InputStream template = Server.class.getClassLoader()
					.getResourceAsStream("application.template.properties")) {
					if (template == null) {
						throw new IllegalStateException(
							"Missing application.properties (filesystem and classpath). Set env vars or bake the file into the jar."
						);
					}
					properties.load(template);
					log.info("Loading config from classpath application.template.properties (override with env vars)");
					return new AppConfiguration(properties);
				}
			}
			properties.load(in);
			log.info("Loading config from classpath application.properties");
			return new AppConfiguration(properties);
		}
	}

	/** Prefer PaaS {@code PORT}, else {@code SERVER_PORT}, else 8090. */
	private static int resolvePort(AppConfiguration configuration) {
		String portEnv = System.getenv("PORT");
		if (portEnv != null && !portEnv.isBlank()) {
			return Integer.parseInt(portEnv.trim());
		}
		return configuration.getInt("SERVER_PORT", 8090);
	}
}
