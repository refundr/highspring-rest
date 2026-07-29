# Testing strategy

## Pyramid

1. **Unit** — `CartPricingServiceTest`  
   Discounts before tax, multi-category totals, empty cart rejection. Fast, no database.

2. **API integration** — `PurchaseApiTest`, `AdminApiTest`  
   Each test opens `TestDatabaseScope` (create DB → Flyway → Jetty on random port → drop DB). Google is stubbed. Covers:

   - Happy-path checkout and totals
   - 401 / 403 / 404 status codes
   - ACID: failed checkout leaves zero purchases
   - Forced 500 writes `api_error_log`
   - ADMIN can load Allure HTML; CUSTOMER gets 403

3. **Reporting** — Allure 3 via `allure-testng` + `allure-maven`  
   Stories/severity annotations tell the quality story for reviewers.

## Why per-test databases

Isolation without Testcontainers complexity, real Postgres + Flyway, and teardown that cannot leak data between tests.

## How to run

```bash
mvn test
mvn -pl api -am allure:serve
```
