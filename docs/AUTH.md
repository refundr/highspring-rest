# Calling the Highspring API (auth + Postman)

## Auth model

Protected endpoints expect:

```http
Authorization: session:{uuid}
Accept: application/json
```

The UUID is an `api_session` row created when Google login finishes (`POST /v1/auth/google/callback/`). It is **not** a JWT and **not** the Remix `__highspring` cookie. The Remix app keeps `sessionId` inside that cookie and adds the `Authorization` header only on server-side fetches.

| Layer | What it holds |
|-------|----------------|
| Remix cookie `__highspring` | JSON copy of session (for the storefront BFF) |
| Header `Authorization: session:…` | What the Jetty API validates |
| Postgres `api_session` | Source of truth (expiry + revoke on logout) |

Logout: `DELETE /v1/auth/logout/` with a valid session header deletes the DB row. After that, the same UUID returns **401**.

Admin routes (`/v1/admin/…`) also require `role = ADMIN` (email listed in `ADMIN_EMAILS` at Google login time).

## Get a `sessionId` for Postman

### Option A — Google code exchange (API only)

1. Start the API (`http://127.0.0.1:8090`).
2. In Postman, open **Auth → Get Google auth URL**.
3. Open the returned `uri` in a browser and sign in.
4. Google redirects to `redirectUri` (default `http://localhost:3000/auth/callback?code=…`).
   - If the Remix app is running, it may consume the code immediately — use Option B, or temporarily copy the `code` from the browser address bar before the app redirects away.
5. Paste `code` into **Auth → Exchange Google code for session** (same `redirectUri` as step 2).
6. The Tests script saves `sessionId` on the collection. Other requests use `Authorization: session:{{sessionId}}`.

`redirectUri` must match Google Cloud Console **and** the value used when building the auth URL (collection variable `redirectUri`).

### Option B — Sign in via Remix, paste `sessionId`

1. Run the storefront, sign in with Google.
2. Call `GET /v1/me/` from the Remix server (or use any authenticated response body that includes `sessionId`), **or** temporarily log `sessionId` after login in `auth.callback`.
3. Paste that UUID into the collection variable `sessionId`.

### Option C — Integration tests

API tests use a stub OAuth provider and create sessions without Google. That path is for TestNG, not interactive Postman.

## Postman collection

File: [docs/postman/Highspring_API.postman_collection.json](postman/Highspring_API.postman_collection.json)

**Import:** Postman → Import → choose that file.

**Collection variables**

| Variable | Default | Purpose |
|----------|---------|---------|
| `baseUrl` | `http://127.0.0.1:8090` | API origin |
| `sessionId` | _(empty)_ | Filled by login request Tests script |
| `redirectUri` | `http://localhost:3000/auth/callback` | Must match Google OAuth client |
| `productId` | _(empty)_ | Set by **List products** Tests script |
| `purchaseId` | _(empty)_ | Set by checkout / create purchase |
| `errorId` | _(empty)_ | Set by **List error logs** |

**Suggested happy path**

1. Auth URL → exchange code (or paste `sessionId`)
2. Get me
3. List products
4. Add cart item → Get cart → Checkout cart
5. (Admin account) Totals / errors / boom if enabled

## CORS note

Browsers calling the API directly need an origin listed in `CORS_ORIGINS`. Postman is not a browser, so CORS does not apply. The Remix app talks to the API from Node (BFF), not from page JS with the bearer token.

## Status codes

See JavaDoc `HttpStatusGuide` / Admin → API docs, or [ARCHITECTURE.md](ARCHITECTURE.md).
