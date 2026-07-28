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
 * Starts the Highspring shopping cart API.
 * Copy application.template.properties to application.properties and fill in your settings first.
 */
public final class Server {

	private static final Logger log = LoggerFactory.getLogger(Server.class);

	private Server() {
	}

	public static void main(String[] args) throws Exception {
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
			Thread.currentThread().join();
		}
	}
}
