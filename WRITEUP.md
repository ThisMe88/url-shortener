# Write-up

> Draft to review and make my own before submitting — the facts below match the
> commit history, but the framing and opinions should be checked against my own.

## 1. What I asked the AI to do vs. what I decided myself

I drove the project through explicit decisions and had the AI (Claude Code) do
the implementation phase by phase, reviewing each phase's diff and running it
against a real PostgreSQL before moving on.

**I decided:**

- **Short-code strategy:** random base62, 7 chars, with a DB unique constraint
  and bounded retry — chosen over a monotonic counter because non-enumerability
  matters for a public redirector.
- **Duplicate-URL policy:** idempotent for generated codes (same URL → same
  code), custom aliases exempt.
- **Scope of analytics:** a click counter plus `GET /{code}/stats`, not a
  per-click event table.
- **Test infrastructure:** Testcontainers for integration tests, real local
  Postgres for development.
- **HTTP semantics:** `301` per the brief; `201` on create / `200` on idempotent
  repeat; `409` for a taken alias vs `422` for a malformed one.
- **Data model:** single `short_url` table, SHA-256 hash column + partial unique
  index to back the dedupe.

**The AI produced:** the Maven/Spring Boot scaffold, the Flyway migration, the
entity/repository, `UrlNormalizer` / `AliasPolicy` / `CodeGenerator`, the three
controllers and the `@RestControllerAdvice`, and the test suite (unit, web-slice,
Testcontainers integration). I reviewed each phase, adjusted wording and
structure, and verified behaviour with `curl` against local Postgres.

## 2. Where I overrode / corrected / threw away AI output

- **Project scaffold:** the plan was Spring Initializr, but start.spring.io now
  only offers Spring Boot ≥ 4.0. I kept the wrapper it generated and threw away
  the rest, hand-writing `pom.xml` on Boot 3.3.4 (the required stack).
- **Test strategy under no Docker:** the first `contextLoads` test failed because
  this machine has no Docker daemon. Rather than drop Testcontainers, I moved the
  container into an `AbstractIntegrationTest` base with
  `@Testcontainers(disabledWithoutDocker = true)` so the suite is *skipped*, not
  *failed*, where Docker is absent, and still runs in full on CI / the reviewer's
  machine. (Committed as a separate `fix:` commit.)
- **Transaction boundary:** I did not want the shorten flow wrapped in one
  transaction that a caught constraint violation would poison. I had it
  restructured so each repository call is its own transaction and the DB
  constraints (`ux_short_url_code`, the partial hash index) are the arbiter.

## 3. The biggest trade-offs and the alternatives

1. **Random codes vs. counter + base62.** A counter makes collisions impossible
   by construction and needs no retry, but it leaks link volume and is trivially
   enumerable. I took random codes: collision probability is ~10⁻²⁸ for five
   consecutive clashes at 10M rows, and the unique constraint still guarantees
   correctness. Cost: a (vanishingly rare) retry and a `500` if all retries clash.
2. **Idempotent duplicate handling.** Returning the same code for a repeated URL
   keeps the table small and the API predictable, but a caller can't get two
   codes for one destination to A/B two campaigns. Mitigation: custom aliases are
   exempt, so that use case still has an answer. The alternative — always mint a
   new code — was rejected as the default but is a one-line change.
3. **`301` and its caching.** Per the brief, but browsers cache `301`
   aggressively, so `clickCount` undercounts repeat visits and a destination is
   effectively immutable once served. `302`/`307` would keep every hit flowing
   through the service at the cost of redirect latency; the right call if
   analytics fidelity outranked speed.

## 4. What's missing / what I'd do with another day

- **Abuse controls:** rate limiting on `POST /shorten`, and an allow/deny list or
  safe-browsing check on destination URLs (open redirector risk).
- **Auth:** API keys so `stats` and alias creation aren't world-open.
- **Richer analytics:** a `click_event` table (timestamp, referrer, UA) behind a
  feature flag, with a documented PII/retention stance — and probably a `302` if
  I did this.
- **Lifecycle:** link expiry / soft-delete, and an idempotency story for aliases
  (currently a duplicate alias is a hard `409`).
- **DX:** springdoc/OpenAPI, a `docker-compose` target that also runs the app,
  and Micrometer counters for generated-vs-deduped and collision-retry rates.
- **Normalization depth:** optionally canonicalize query-param order and
  percent-encoding — deliberately left out to avoid collapsing distinct links.
