package ca.refundr.highspring.common.config;

import com.google.common.base.Preconditions;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Reads settings from a properties file (and environment variables as overrides).
 * Think of this as the app's "settings sheet" — database URL, Google login keys, who gets alert emails.
 */
public final class AppConfiguration {

	private final Properties properties;

	public AppConfiguration(Path propertiesFile) throws IOException {
		Preconditions.checkNotNull(propertiesFile, "propertiesFile");
		this.properties = new Properties();
		try (InputStream in = Files.newInputStream(propertiesFile)) {
			properties.load(in);
		}
	}

	public AppConfiguration(Properties properties) {
		Preconditions.checkNotNull(properties, "properties");
		this.properties = new Properties();
		this.properties.putAll(properties);
	}

	public String getString(String key) {
		Preconditions.checkNotNull(key, "key");
		String env = System.getenv(key);
		if (env != null && !env.isBlank()) {
			return env;
		}
		String value = properties.getProperty(key);
		Preconditions.checkArgument(value != null && !value.isBlank(), "Missing required config: %s", key);
		return value.trim();
	}

	public String getString(String key, String defaultValue) {
		Preconditions.checkNotNull(key, "key");
		String env = System.getenv(key);
		if (env != null && !env.isBlank()) {
			return env.trim();
		}
		String value = properties.getProperty(key);
		if (value == null || value.isBlank()) {
			return defaultValue;
		}
		return value.trim();
	}

	public int getInt(String key, int defaultValue) {
		String raw = getString(key, String.valueOf(defaultValue));
		return Integer.parseInt(raw);
	}

	public Set<String> getCsvSet(String key) {
		String raw = getString(key, "");
		if (raw.isBlank()) {
			return Set.of();
		}
		return Arrays.stream(raw.split(","))
			.map(String::trim)
			.filter(s -> !s.isEmpty())
			.map(String::toLowerCase)
			.collect(Collectors.toUnmodifiableSet());
	}
}
