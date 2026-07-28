# Highspring REST API

Shopping cart backend for an interview exercise: catalog discounts, sales tax, Google sign-in, admin tooling, and Allure 3 test reports.

## What it does

- Lists seeded products (read-only catalog)
- Checks out a cart: category discount **before** 8.5% sales tax
- Saves the whole purchase in one database transaction (ACID)
- Signs users in with Google OAuth (code lives in this API)
- Roles: `CUSTOMER` and `ADMIN` (emails in `ADMIN_EMAILS` become admins)
- On unexpected **500** errors: stack trace is saved to `api_error_log` **and** emailed to the developer
- Admins can view totals, errors, and the published Allure report

## Modules

| Module | Role |
|--------|------|
| `common` | Config, mail, error reporting interfaces |
| `domain` | Request/response DTOs |
| `database` | Flyway, JOOQ access, per-test Postgres databases |
| `api` | Jetty server, resources, OAuth, tests |

## Prerequisites

- Java 21+
- Maven 3.9+
- Postgres listening locally (example: port `5436`)
- A Google OAuth client (Web application) with redirect URI matching the Remix app

Create the database once:

```bash
psql -h localhost -p 5436 -U postgres -c "CREATE DATABASE highspring;"
```

## Configuration

```bash
cp api/src/main/resources/application.template.properties api/src/main/resources/application.properties
```

Fill in `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `GOOGLE_REDIRECT_URI`, `ADMIN_EMAILS`, and `DEVELOPER_ALERT_EMAIL`.

`MAIL_MODE=logging` prints alert emails to the log (good for local). Set `MAIL_MODE=smtp` for real SMTP.

## Run the API

```bash
mvn -pl api -am package -DskipTests
java -jar api/target/api-1.0-SNAPSHOT.jar
```

Or from an IDE: run `ca.refundr.highspring.api.Server`.

Default: `http://127.0.0.1:8080`

## Tests + Allure 3

Tests create a throwaway Postgres database (`highspring_testN`), migrate with Flyway, run HTTP checks against embedded Jetty, then drop the database.

```bash
mvn test
mvn -pl api allure:serve
# publish HTML for the admin UI:
mvn -pl api allure:report verify
```

Published report directory: `api/published-allure/` (served at `/v1/admin/allure/` for **ADMIN** sessions).

## IntelliJ IDEA

1. **Open the project root** (`highspring-rest`), not a submodule folder — File → Open → select the folder that contains the root `pom.xml`.
2. When prompted, choose **Trust Project** and **Load Maven Project** / **Import as Maven**.
3. Set Project SDK to **JDK 21** (or 22): File → Project Structure → Project → SDK.
4. Wait for Maven sync to finish (bottom-right progress). Then use run config **Highspring API**.

If sources stay red: Maven tool window → reload (circular arrows), or right-click root `pom.xml` → Maven → Reload project.

Copy config first:

```bash
cp api/src/main/resources/application.template.properties api/src/main/resources/application.properties
```
