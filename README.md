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

`api/src/main/resources/application.properties` is committed so a clone can start without copying files.
Blank reference copy: `application.template.properties` (same keys).

Override any value with an environment variable of the same name if needed.

`MAIL_MODE=logging` prints alert emails to the log. The committed file uses `MAIL_MODE=smtp`.

**API clients:** import the [Postman collection](docs/postman/Highspring_API.postman_collection.json) and read [docs/AUTH.md](docs/AUTH.md) for `Authorization: session:{uuid}`.

**Codebase map?** Read [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) and the JavaDoc on `RootResource` / `Version1Resource` (custom HTTP resource tree).

## Run the API

```bash
mvn -pl api -am package -DskipTests
java -jar api/target/api-1.0-SNAPSHOT.jar
```

Or from an IDE: run `ca.refundr.highspring.api.Server`.

Default: `http://127.0.0.1:8090`

## Tests + admin reports (Allure + JavaDoc)

Reports are **not** in git. After a fresh clone, generate them once (Postgres must be up), then restart the API:

```bash
mvn -pl api -am verify && mvn javadoc:aggregate
```

That runs tests, builds the Allure HTML, copies it to `api/published-allure/`, and writes JavaDoc to `api/published-javadoc/apidocs/`.

Admin UI (Remix) proxies:

- `/admin/allure/` → API `/v1/admin/allure/`
- `/admin/javadoc/` → API `/v1/admin/javadoc/`

If the admin page says the report was not found, the API process cannot see those folders (wrong cwd or reports never generated). Startup logs print the resolved Allure/JavaDoc paths.

`-am` builds dependent modules (`common`, `domain`, `database`) with `api`. Without it, Maven looks for those jars in the local repo and fails.

Tests only (no report publish):

```bash
mvn test
```

## IntelliJ IDEA

Maven compiles from the CLI (`mvn -pl api -am -DskipTests compile`). If the IDE looks broken, it usually means IntelliJ never finished importing the **parent** Maven project, or the Project SDK points at the wrong JDK.

**This repo expects JDK 21.** IntelliJ should offer to configure SDK `21` when you open the project (`languageLevel` / `project-jdk-name` in `.idea/misc.xml`).

1. **File → Open** the folder that contains the root `pom.xml` (`highspring-rest`).  
   Do **not** open only `api/`, and do **not** open a multi-root workspace that mixes this repo with the Remix client.
2. When prompted: **Trust Project**, then **Load as Maven Project** / **Import Maven Project**.
3. If IntelliJ shows **Project SDK is not defined** (or build fails with `cannot execute binary file`):
   - **File → Project Structure → Project → SDK** → **Add JDK…** or **Download JDK…** → **21**
   - Use a **macOS** JDK (Mach-O), e.g. JetBrains Runtime / Temurin / Homebrew OpenJDK.  
     Do **not** point at a Linux JDK under something like `/Users/…/java/jdk-23*` (those fail with `cannot execute binary file` on Apple Silicon).
   - **File → Project Structure → Platform Settings → SDKs**: remove any broken/non-mac JDK entries.
4. **Settings → Build, Execution, Deployment → Build Tools → Maven → Runner**
   - **JRE:** same JDK 21 (or “Use Project JDK”)
5. Open the **Maven** tool window → click **Reload All Maven Projects** (circular arrows). Wait until sync finishes (bottom-right).
6. Confirm modules appear: `highspring`, `common`, `domain`, `database`, `api`.
7. Run config **Highspring API** (`ca.refundr.highspring.api.Server`, working directory `api/`).

If sources stay red after that:

- Right-click root `pom.xml` → **Maven → Reload project**
- Or **File → Invalidate Caches → Invalidate and Restart**, then reload Maven again

Config is already present at `api/src/main/resources/application.properties` (committed for local demos).
