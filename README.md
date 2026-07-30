# Highspring REST API

## Engineering principles

- Favor readability over brevity
- Favor libraries over frameworks
- Avoid the use of any technology that introduces "magic" (an element of surprise) into the software development / debugging process
- Given the choice between build-time code generation or runtime bytecode generation, we favor the former. Code generation creates source code that can be read and debugged, unlike bytecode generation.
    - favour generated code that can be read and debugged over bytecode injected at runtime
- We invest the necessary time to ensure that our software is easy to maintain over the long haul.
- We add tests, refactor, and document our work as we go along, not after the fact.
- Time estimates include this work as an inseparable part of implementing a new feature.
- We work as part of a team. When you write (and document) code, do it with your teammates in mind.

Full copy: [docs/PRINCIPLES.md](docs/PRINCIPLES.md).

Shopping cart backend for an interview exercise: catalog discounts, sales tax, Google sign-in, admin tooling, and Allure 3 test reports.

## What it does

- Lists seeded products (read-only catalog)
- Checks out a cart: category discount **before** 8.5% sales tax
- Saves the whole purchase in one database transaction (ACID)
- Signs users in with Google OAuth (code lives in this API)
- Roles: `CUSTOMER` and `ADMIN` (emails in `ADMIN_EMAILS` become admins)
- On unexpected **500** errors: stack trace is saved to `api_error_log` **and** emailed to the developer
- Admins can view totals, errors, and the published Allure report
- Demo 500: with `ENABLE_BOOM_ENDPOINT=true`, `GET /v1/admin/boom/` (ADMIN session) throws on purpose

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

**API clients:** import the [Postman collection](docs/postman/Highspring_API.postman_collection.json) and read [docs/AUTH.md](docs/AUTH.md) for `Authorization: session:{uuid}`.

**Codebase map?** Read [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) and the JavaDoc on `RootResource` / `Version1Resource` (custom HTTP resource tree).

## Run the API

```bash
mvn -pl api -am package -DskipTests
java -jar api/target/api-1.0-SNAPSHOT.jar
```

Or from an IDE: run `ca.refundr.highspring.api.Server`.

Default: `http://127.0.0.1:8090`

## Tests + Allure 3

Tests create a throwaway Postgres database (`highspring_testN`), migrate with Flyway, run HTTP checks against embedded Jetty, then drop the database.

```bash
mvn test
mvn -pl api -am allure:report verify
```

Published report directory: `api/published-allure/` (served at `/v1/admin/allure/` for **ADMIN** sessions).

JavaDoc (with HTTP status / error-code guide): `mvn javadoc:aggregate` → `api/published-javadoc/apidocs/` (served at `/v1/admin/javadoc/` for **ADMIN**).

`-am` builds dependent modules (`common`, `domain`, `database`) with `api`. Without it, Maven looks for those jars in the local repo and fails.

## IntelliJ IDEA

Maven compiles from the CLI (`mvn -pl api -am -DskipTests compile`). If the IDE looks broken, it usually means IntelliJ never finished importing the **parent** Maven project.

1. **File → Open** the folder that contains the root `pom.xml` (`highspring-rest`).  
   Do **not** open only `api/`, and do **not** open a multi-root workspace that mixes this repo with the Remix client.
2. When prompted: **Trust Project**, then **Load as Maven Project** / **Import Maven Project**.
3. **File → Project Structure → Project**
   - **SDK:** JDK **21** (this repo’s `java.version`)
   - **Language level:** 21
4. **Settings → Build, Execution, Deployment → Build Tools → Maven → Runner**
   - **JRE:** same JDK 21 (or “Use Project JDK”)
5. Open the **Maven** tool window → click **Reload All Maven Projects** (circular arrows). Wait until sync finishes (bottom-right).
6. Confirm modules appear: `highspring`, `common`, `domain`, `database`, `api`.
7. Run config **Highspring API** (`ca.refundr.highspring.api.Server`, working directory `api/`).

If sources stay red after that:

- Right-click root `pom.xml` → **Maven → Reload project**
- Or **File → Invalidate Caches → Invalidate and Restart**, then reload Maven again

Copy config first:

```bash
cp api/src/main/resources/application.template.properties api/src/main/resources/application.properties
```
