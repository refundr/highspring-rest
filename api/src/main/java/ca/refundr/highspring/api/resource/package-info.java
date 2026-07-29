/**
 * HTTP “resource tree” for Highspring.
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
 * <h2>How a request is handled</h2>
 *
 * <p>This API does <strong>not</strong> use Spring MVC annotations like {@code @GetMapping}.
 * Instead it builds a small tree of resource objects, one segment of the URL at a time.
 *
 * <ol>
 *   <li>{@link ca.refundr.highspring.api.jetty.RequestFilter} receives every HTTP call.</li>
 *   <li>It creates a {@link ca.refundr.highspring.api.resource.RootResource} and asks
 *       {@code root.getByPath("v1/products/")} (path without the leading slash).</li>
 *   <li>{@code getByPath} walks the tree: Root → {@link ca.refundr.highspring.api.resource.version1.Version1Resource}
 *       → a leaf like ProductsResource.</li>
 *   <li>The matching resource’s {@code httpGet}/{@code httpPost}/… method runs and returns a
 *       {@link ca.refundr.highspring.api.util.ServerResponse} that writes JSON/text.</li>
 * </ol>
 *
 * <h2>Example walk for {@code GET /v1/products/}</h2>
 *
 * <pre>
 *   RootResource            relativePath = ""
 *     └─ Version1Resource   relativePath = "v1/"
 *          └─ ProductsResource relativePath = "products/"
 * </pre>
 *
 * <p>Each parent’s {@code getDescendantByPath} lists its children. The first child whose
 * {@code getByPath} matches wins. Nested IDs (e.g. {@code /v1/purchases/{uuid}/}) use helpers
 * like {@code getDescendantFromChildByUuid}.
 *
 * <h2>Auth</h2>
 *
 * <p>Protected handlers call {@code requireSessionUser()} or {@code requireAdmin()}.
 * The client sends {@code Authorization: session:{uuid}}. Failures throw
 * {@link ca.refundr.highspring.api.util.exceptions.RequestFailedException} (expected 4xx).
 */
package ca.refundr.highspring.api.resource;
