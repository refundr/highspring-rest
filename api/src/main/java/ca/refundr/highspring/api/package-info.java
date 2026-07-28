/**
 * Highspring shopping cart HTTP API (Jetty + hand-rolled resource routing).
 *
 * <p><b>Modules nearby:</b> {@code domain} = JSON DTOs, {@code database} = Flyway/JOOQ,
 * {@code common} = config/mail/error reporting.
 *
 * <p><b>Boot:</b> {@link ca.refundr.highspring.api.Server} → Jetty →
 * {@link ca.refundr.highspring.api.jetty.RequestFilter} → resource tree under
 * {@link ca.refundr.highspring.api.resource}.
 */
package ca.refundr.highspring.api;
