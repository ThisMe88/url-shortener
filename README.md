# url-shortener

A Spring Boot REST service that turns long URLs into short codes and redirects
visitors to the original link. Supports custom aliases, idempotent handling of
duplicate URLs, collision-resistant code generation, and basic click analytics.

**Stack:** Java 17, Spring Boot 3.3.4, Maven, PostgreSQL, Flyway, JUnit 5, Testcontainers.

## Status

Built in phases; see the commit history. Current phase: **Phase 0 – project scaffold**.

## Prerequisites

- JDK 17
- Docker (for local Postgres and for the integration test suite via Testcontainers)

## Run locally

```bash
# 1. Start Postgres
docker compose up -d

# 2. Run the app (uses the Maven wrapper; no local Maven needed)
./mvnw spring-boot:run
```

The service listens on `http://localhost:8080`.

Configuration (environment variables, with defaults):

| Variable        | Default                                             | Purpose                          |
|-----------------|-----------------------------------------------------|----------------------------------|
| `DB_URL`        | `jdbc:postgresql://localhost:5432/urlshortener`     | JDBC URL                         |
| `DB_USERNAME`   | `urlshortener`                                      | DB user                          |
| `DB_PASSWORD`   | `urlshortener`                                      | DB password                      |
| `APP_BASE_URL`  | `http://localhost:8080`                             | Base URL used to build short links |

## Test

```bash
./mvnw test
```

Integration tests start a throwaway PostgreSQL container via Testcontainers, so
Docker must be running.

## API

Documented as endpoints land in later phases.

## Design notes

Collision analysis, duplicate-URL policy, and custom-alias rules are documented
here as the corresponding phases are implemented. See also `WRITEUP.md`.
