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

Expected 4xx failures use `RequestFailedException` and do not alert. Unexpected exceptions reach `RequestFilter`, which:

1. Writes a safe 500 body to the client
2. Calls `CompositeErrorReporter` → `DatabaseErrorReporter` (stack trace in `api_error_log`) + `EmailErrorReporter` (developer email) + logging + Sentry stub

Reporter failures are caught so they never replace the original 500 response.

## Admin Allure access

Allure HTML is published into `ALLURE_REPORT_DIR` and served under `/v1/admin/allure/` after session + `ADMIN` role checks. The Remix admin UI proxies that path with the session header so browsers can display the report.
