/**
 * Highspring shopping cart HTTP API (Jetty + hand-rolled resource routing).
 *
 * <h2>Engineering principles</h2>
 *
 * <ul>
 *   <li>Favor readability over brevity</li>
 *   <li>Favor libraries over frameworks</li>
 *   <li>Avoid the use of any technology that introduces "magic" (an element of surprise) into
 *       the software development / debugging process</li>
 *   <li>Given the choice between build-time code generation or runtime bytecode generation, we favor
 *       the former. Code generation creates source code that can be read and debugged, unlike bytecode
 *       generation.</li>
 *   <li>Favour generated code that can be read and debugged over bytecode injected at runtime</li>
 *   <li>We invest the necessary time to ensure that our software is easy to maintain over the long haul.</li>
 *   <li>We add tests, refactor, and document our work as we go along, not after the fact.</li>
 *   <li>Time estimates include this work as an inseparable part of implementing a new feature.</li>
 *   <li>We work as part of a team. When you write (and document) code, do it with your teammates in mind.</li>
 * </ul>
 *
 * <p>See also {@code docs/PRINCIPLES.md}.
 *
 * <p><b>Modules nearby:</b> {@code domain} = JSON DTOs, {@code database} = Flyway/JOOQ,
 * {@code common} = config/mail/error reporting.
 *
 * <p><b>Boot:</b> {@link ca.refundr.highspring.api.Server} → Jetty →
 * {@link ca.refundr.highspring.api.jetty.RequestFilter} → resource tree under
 * {@link ca.refundr.highspring.api.resource}.
 */
package ca.refundr.highspring.api;
