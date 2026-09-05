# url-shortener

A Spring Boot REST service that turns long URLs into short codes and redirects
visitors to the original link. Supports custom aliases, idempotent handling of
duplicate URLs, collision-resistant code generation, and basic click analytics.

**Stack:** Java 17 · Spring Boot 3.3.4 · Maven · PostgreSQL · Flyway · JPA/Hibernate
· JUnit 5 · Testcontainers.

---

## Prerequisites

- **JDK 17** (`JAVA_HOME` pointing at a 17 JDK)
- **PostgreSQL** reachable on `localhost:5432` — either a local install or the
  bundled `docker compose` service below
- **Docker** — only for the integration test suite (Testcontainers). Unit and
  web-slice tests need no Docker; the integration tests are *skipped*, not
  failed, when no Docker daemon is present.

The Maven Wrapper (`./mvnw`) is checked in, so a local Maven install is **not**
required.

## Database setup

### Option A — Docker

```bash
docker compose up -d
```

Brings up `postgres:16-alpine` with database `urlshortener`, user
`urlshortener`, password `urlshortener` (matches the app defaults).

### Option B — existing local PostgreSQL

```sql
CREATE ROLE urlshortener LOGIN PASSWORD 'urlshortener';
CREATE DATABASE urlshortener OWNER urlshortener;
```

Flyway creates the schema automatically on first start.

## Run

```bash
./mvnw spring-boot:run
```

Service starts on `http://localhost:8080`.

### Configuration

All settings are environment-overridable; defaults in `application.yml`:

| Variable        | Default                                          | Purpose                             |
|-----------------|--------------------------------------------------|-------------------------------------|
| `DB_URL`        | `jdbc:postgresql://localhost:5432/urlshortener`  | JDBC URL                            |
| `DB_USERNAME`   | `urlshortener`                                   | DB user                            |
| `DB_PASSWORD`   | `urlshortener`                                   | DB password                        |
| `APP_BASE_URL`  | `http://localhost:8080`                          | Origin used to render short links   |
| `APP_CODE_LENGTH` *(app.code.length)* | `7`                        | Generated code length              |

## Test

```bash
./mvnw test
```

- **Unit + web-slice tests** always run.
- **Integration tests** (`*IntegrationTest`, `ShortUrlRepositoryTest`) start a
  throwaway PostgreSQL container via Testcontainers. With no Docker available
  they are skipped and the build still passes; start Docker to run them.

---

## API

Base URL: `http://localhost:8080`

### `POST /shorten`

Request:

```json
{ "url": "https://example.com/some/long/path?ref=abc", "alias": "my-link" }
```

`alias` is optional. Responses:

| Status | When | Body |
|--------|------|------|
| `201 Created` | a new mapping was created (`Location` header set to the short link) | `{ code, shortUrl, originalUrl }` |
| `200 OK` | this URL already had a generated code — the existing one is returned (idempotent) | same shape |
| `400 Bad Request` | missing/blank URL, malformed URL, non-http(s) scheme, no host, > 2048 chars | RFC 7807 problem |
| `409 Conflict` | requested `alias` is already taken | RFC 7807 problem |
| `422 Unprocessable Entity` | `alias` fails the charset/length rules or is reserved | RFC 7807 problem |

```bash
curl -X POST localhost:8080/shorten \
  -H 'Content-Type: application/json' \
  -d '{"url":"https://example.com/docs?a=1#top"}'
# 201  {"code":"eB7rNfu","shortUrl":"http://localhost:8080/eB7rNfu","originalUrl":"https://example.com/docs?a=1"}
```

### `GET /{code}`

`301 Moved Permanently` with `Location: <original url>`. `404` for an unknown
code. Each hit increments `clickCount` and updates `lastAccessedAt`.

```bash
curl -i localhost:8080/eB7rNfu
# HTTP/1.1 301
# Location: https://example.com/docs?a=1
```

### `GET /{code}/stats`

```json
{
  "code": "eB7rNfu",
  "originalUrl": "https://example.com/docs?a=1",
  "customAlias": false,
  "clickCount": 3,
  "createdAt": "2026-09-05T22:29:08.418566Z",
  "lastAccessedAt": "2026-09-05T22:31:10.735781Z"
}
```

`404` for an unknown code.

### `GET /actuator/health`

Liveness/readiness for the service and its datastore.

---

## Design decisions

### Short-code generation — why it won't collide

Codes are 7 characters drawn from a 62-symbol base62 alphabet (`A–Z a–z 0–9`)
using `SecureRandom`. Every character is *unreserved* in a URI path (RFC 3986),
so a code never needs percent-encoding; separators (`- _ ~ .`) are deliberately
excluded so codes stay copy-paste/double-click friendly.

The code space is `62^7 ≈ 3.52 × 10^12`. For a freshly generated code the
probability of clashing with an existing row is `n / 62^7`, where `n` is the
number of rows already stored:

| Stored URLs | P(single collision) | P(5 consecutive collisions) |
|-------------|---------------------|------------------------------|
| 1 million   | ~2.8 × 10⁻⁷         | ~1.8 × 10⁻³³                  |
| 10 million  | ~2.8 × 10⁻⁶         | ~1.8 × 10⁻²⁸                  |

`UniqueCodeGenerator` pre-checks with `existsByCode` and re-rolls up to
`app.code.max-generation-attempts` (default 5) before giving up with a `500`.
Independently, the `ux_short_url_code` **unique constraint** is the real
arbiter: even under a race where two requests generate the same code, the
database rejects the second `INSERT`, so a duplicate can never be persisted —
worst case is one wasted round trip.

A monotonic counter + base62 encoding would make collisions *impossible* by
construction, but it leaks the total number of links and makes codes trivially
enumerable (scrapeable). Random codes trade a vanishing collision probability
for non-enumerability, which is the better default for a public redirector.

### Duplicate URLs — idempotent for generated codes

Shortening the same URL twice (no alias) returns the **same** code: `201` the
first time, `200` on repeats. Implemented with a SHA-256 hash of the *normalized*
URL (`original_url_hash`) and a **partial unique index**
`ux_short_url_hash_generated ... WHERE custom_alias = FALSE`.

Rationale: for a plain "make this shorter" call, returning a stable code is the
least surprising behaviour and keeps the table small. The cost is that a caller
cannot obtain two different codes for one destination to measure two campaigns
separately — for that, use a custom alias (see below).

### Custom aliases

`alias` must match `^[A-Za-z0-9_-]{3,16}$` and must not be a reserved word
(`shorten`, `stats`, `actuator`, `health`, …), so an alias can never shadow an
API route. Aliases are **exempt** from the duplicate-URL rule: you can always
mint `sho.rt/spring-sale` for a URL that already has a generated code, and each
alias is its own row. A taken alias returns `409`; a malformed/reserved one
returns `422` (the request is well-formed JSON but semantically unprocessable).

### URL validation & normalization

Accepted: an absolute `http`/`https` URI with a host, ≤ 2048 chars. Normalization
before hashing/storage: lower-case scheme and host, drop a default port
(`80`/`443`), strip the fragment. Path and query are kept **verbatim** — they are
frequently significant and over-normalizing (e.g. sorting query params, trimming
trailing slashes) risks collapsing genuinely different links.

### `301` vs `302`

The spec asks for `301`. Consequence: browsers cache it hard, so a code's
destination is effectively immutable once served and the `clickCount` undercounts
repeat visits from the same client. A `302` (or `307`) would keep every hit
flowing through the service at the cost of redirect latency — a reasonable change
if analytics accuracy mattered more than redirect speed.

### Concurrency

`ShortenService` is intentionally **not** one big transaction. Each repository
call is its own transaction and the DB constraints are the source of truth:

- two requests racing to shorten the *same new URL* → the partial unique index
  rejects the loser's insert; it catches `DataIntegrityViolationException`,
  re-reads, and returns the winner's row (still `200`, still idempotent);
- two requests racing on the *same alias* → `ux_short_url_code` rejects the
  loser; it is translated to `409`.

Redirect hit-counting is a single atomic
`UPDATE ... SET click_count = click_count + 1`, so concurrent redirects of one
code cannot lose increments.

### Data model

One table, `short_url`:

| Column | Type | Notes |
|--------|------|-------|
| `id` | `bigint` identity | PK, internal |
| `code` | `varchar(16)` | unique (`ux_short_url_code`) |
| `original_url` | `text` | normalized |
| `original_url_hash` | `varchar(64)` | SHA-256 hex; partial-unique where `custom_alias = false` |
| `custom_alias` | `boolean` | generated vs user alias |
| `click_count` | `bigint` | redirect counter |
| `created_at` | `timestamptz` | |
| `last_accessed_at` | `timestamptz` | nullable |

---

## Project layout

```
src/main/java/com/example/urlshortener
├── config/     AppProperties, CodeProperties  (@ConfigurationProperties)
├── domain/     ShortUrl entity, ShortUrlRepository
├── service/    UrlNormalizer, AliasPolicy, CodeGenerator, UniqueCodeGenerator,
│               ShortenService, RedirectService, StatsService, exceptions
└── web/        ShortenController, RedirectController, StatsController,
                GlobalExceptionHandler, dto/
src/main/resources/db/migration/V1__create_short_url.sql
```

See `WRITEUP.md` for the build narrative and trade-off discussion.
