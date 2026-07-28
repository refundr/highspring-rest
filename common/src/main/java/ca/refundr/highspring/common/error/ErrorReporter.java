package ca.refundr.highspring.common.error;

/**
 * Pluggable place to send serious server failures (500s).
 * One implementation can email a developer; another can write to the database;
 * later you can add Sentry without changing the rest of the app.
 */
public interface ErrorReporter {
	void report(ErrorReport report);
}
