/**
 * HTTP “resource tree” for Highspring.
 *
 * <h2>How a request is handled (start here if you are new)</h2>
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
