package ca.refundr.highspring.common.config;

import java.net.URI;
import java.util.Optional;

/**
 * Normalizes database connection settings for local JDBC and cloud hosts (e.g. Render).
 */
public final class DatabaseUrls {

	private DatabaseUrls() {
	}

	/**
	 * Render often provides {@code postgresql://user:pass@host/db}; Hikari wants
	 * {@code jdbc:postgresql://host/db} plus separate username/password.
	 */
	public static String toJdbcUrl(String raw) {
		if (raw == null || raw.isBlank()) {
			return raw;
		}
		String url = raw.trim();
		if (url.startsWith("jdbc:")) {
			return url;
		}
		if (url.startsWith("postgres://")) {
			url = "postgresql://" + url.substring("postgres://".length());
		}
		if (!url.startsWith("postgresql://")) {
			return raw.trim();
		}
		URI uri = URI.create(url);
		String host = uri.getHost();
		int port = uri.getPort() > 0 ? uri.getPort() : 5432;
		String path = uri.getPath() == null || uri.getPath().isBlank() ? "" : uri.getPath();
		return "jdbc:postgresql://" + host + ":" + port + path;
	}

	public static Optional<String> usernameFromUrl(String raw) {
		return userInfoPart(raw, 0);
	}

	public static Optional<String> passwordFromUrl(String raw) {
		return userInfoPart(raw, 1);
	}

	private static Optional<String> userInfoPart(String raw, int index) {
		if (raw == null || raw.isBlank() || raw.startsWith("jdbc:")) {
			return Optional.empty();
		}
		String url = raw.trim();
		if (url.startsWith("postgres://")) {
			url = "postgresql://" + url.substring("postgres://".length());
		}
		if (!url.startsWith("postgresql://")) {
			return Optional.empty();
		}
		URI uri = URI.create(url);
		String userInfo = uri.getUserInfo();
		if (userInfo == null || userInfo.isBlank()) {
			return Optional.empty();
		}
		String[] parts = userInfo.split(":", 2);
		if (index >= parts.length) {
			return Optional.empty();
		}
		return Optional.of(parts[index]);
	}
}
