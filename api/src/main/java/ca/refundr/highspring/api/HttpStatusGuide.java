package ca.refundr.highspring.api;

/**
 * REST API HTTP status and error-code guide for Highspring.
 *
 * <p>Open this page from the Admin UI (<strong>API docs / Javadoc</strong>) after publishing
 * with {@code mvn javadoc:aggregate} (or {@code mvn verify}).
 *
 * <h2>How errors are decided</h2>
 *
 * <p>{@link ca.refundr.highspring.api.jetty.RequestFilter} is the single place that maps
 * exceptions to status codes:
 * <ul>
 *   <li>Expected client problems → 4xx, <strong>not</strong> written to {@code api_error_log}</li>
 *   <li>Unexpected server failures → 500 + full stack in {@code api_error_log} + developer email</li>
 * </ul>
 *
 * <h2>Status codes returned by this API</h2>
 *
 * <table border="1" summary="HTTP status codes">
 *   <caption>Highspring HTTP status codes</caption>
 *   <tr><th>Code</th><th>When</th><th>Logged to api_error_log?</th><th>Typical cause</th></tr>
 *   <tr>
 *     <td><b>200</b></td>
 *     <td>Successful GET (and some DELETE JSON bodies)</td>
 *     <td>No</td>
 *     <td>Catalog list, cart read, admin totals, me</td>
 *   </tr>
 *   <tr>
 *     <td><b>201</b></td>
 *     <td>Resource created</td>
 *     <td>No</td>
 *     <td>{@code POST /v1/purchases/}, {@code POST /v1/cart/checkout/}</td>
 *   </tr>
 *   <tr>
 *     <td><b>204</b></td>
 *     <td>Success with empty body</td>
 *     <td>No</td>
 *     <td>{@code DELETE /v1/cart/}, {@code DELETE /v1/admin/errors/{id}/}, OPTIONS</td>
 *   </tr>
 *   <tr>
 *     <td><b>400</b></td>
 *     <td>Bad client input</td>
 *     <td>No</td>
 *     <td>{@link ca.refundr.highspring.api.util.exceptions.BadRequestException}:
 *         invalid JSON, empty cart checkout, invalid quantities, missing fields</td>
 *   </tr>
 *   <tr>
 *     <td><b>401</b></td>
 *     <td>Not signed in / bad session</td>
 *     <td>No</td>
 *     <td>Missing or invalid {@code Authorization: session:{uuid}} header;
 *         failed Google code exchange treated as unauthorized in the callback</td>
 *   </tr>
 *   <tr>
 *     <td><b>403</b></td>
 *     <td>Signed in but not allowed</td>
 *     <td>No</td>
 *     <td>Customer hitting {@code /v1/admin/...}; viewing another user's purchase</td>
 *   </tr>
 *   <tr>
 *     <td><b>404</b></td>
 *     <td>Unknown URL or missing entity</td>
 *     <td>No</td>
 *     <td>No matching resource in the tree; unknown product id; missing purchase/error id;
 *         Allure/Javadoc file not published yet</td>
 *   </tr>
 *   <tr>
 *     <td><b>405</b></td>
 *     <td>Wrong HTTP method or Accept</td>
 *     <td>No</td>
 *     <td>POST to a GET-only resource; client not accepting JSON where required</td>
 *   </tr>
 *   <tr>
 *     <td><b>500</b></td>
 *     <td>Unexpected server failure</td>
 *     <td><b>Yes</b> (full stack) + email</td>
 *     <td>Bugs, DB outages, Google 5xx, {@code GET /v1/admin/boom/} demo</td>
 *   </tr>
 * </table>
 *
 * <h2>Authorization header</h2>
 *
 * <pre>
 *   Authorization: session:00000000-0000-0000-0000-000000000000
 * </pre>
 *
 * <p>Session UUIDs come from {@code POST /v1/auth/google/callback/}.
 *
 * <h2>Where to read next</h2>
 *
 * <ul>
 *   <li>{@link ca.refundr.highspring.api.resource} — custom HTTP resource tree</li>
 *   <li>{@link ca.refundr.highspring.api.resource.version1.Version1Resource} — /v1 route table</li>
 *   <li>{@link ca.refundr.highspring.api.jetty.RequestFilter} — filter + exception policy</li>
 * </ul>
 */
public final class HttpStatusGuide {

	private HttpStatusGuide() {
	}
}
