# Design decisions

## Why Jetty + JOOQ + Flyway (not Spring)

This API mirrors the “libraries over frameworks” style used in refundr-aragorn: explicit request routing, plain constructors, and SQL you can read. That makes failures easier to explain in an interview and keeps the HTTP/status/error pipeline obvious.

## ACID purchases

Checkout inserts one `purchase` row and every `purchase_item` in a **single** JDBC transaction (`autoCommit=false`, commit only on success). If a product is missing or pricing fails, the transaction rolls back and no partial order remains. Isolation is `READ_COMMITTED`. The API returns **201** only after commit (durability for the client).

Line prices are snapshotted on the purchase items so later catalog changes do not rewrite history.

## SOLID seams

- Resources handle HTTP only
- `CartPricingService` owns money math
- Row types own SQL
- `OAuthProvider` hides Google (tests use `StubOAuthProvider`)
- `ErrorReporter` / `MailSender` hide alerting sinks (email today, Sentry stub tomorrow)

## 500 errors: database + email

**Persist and alert only unexpected server failures** (true 500s). Expected client failures stay quiet.

| Exception | HTTP | Saved to `api_error_log`? |
|-----------|------|---------------------------|
| `RequestFailedException` (401/403/…) | 4xx | No |
| `ProductNotFound` | 404 | No |
| `BadRequestException` (validation, bad JSON, empty cart) | 400 | No |
| Everything else (`IllegalStateException`, NPE, SQL, Google 5xx, …) | 500 | **Yes** (full stack) + email |

`RequestFilter` implements that split. Reporter failures are caught so they never replace the original 500 response.

## Persisted shopping cart

Signed-in shoppers get a **server-side** cart (`cart_item` keyed by `user_id`). That is the usual pattern once you have accounts/sessions: the cart survives browser restarts and devices, and checkout is authoritative on the server. Anonymous browsers often use `localStorage` until login (then merge); Highspring requires Google sign-in before shopping, so only the server cart is needed.

Checkout for the UI is `POST /v1/cart/checkout/` (creates the purchase in one transaction and clears the cart). Direct `POST /v1/purchases/` with an item list remains for tests and API clients.

## Admin Allure access

Allure HTML is published into `ALLURE_REPORT_DIR` and served under `/v1/admin/allure/` after session + `ADMIN` role checks. The Remix admin UI proxies that path with the session header so browsers can display the report.
