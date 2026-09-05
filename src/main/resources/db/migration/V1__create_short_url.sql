CREATE TABLE short_url (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code              VARCHAR(16)  NOT NULL,
    original_url      TEXT         NOT NULL,
    original_url_hash VARCHAR(64)  NOT NULL,
    custom_alias      BOOLEAN      NOT NULL DEFAULT FALSE,
    click_count       BIGINT       NOT NULL DEFAULT 0,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    last_accessed_at  TIMESTAMPTZ
);

-- Every short code (generated or custom alias) must be globally unique.
CREATE UNIQUE INDEX ux_short_url_code ON short_url (code);

-- Duplicate-URL handling is idempotent for auto-generated codes only: a given
-- normalized URL maps to at most one generated code. Custom aliases are exempt,
-- so the same URL can still have both a generated code and one or more aliases.
CREATE UNIQUE INDEX ux_short_url_hash_generated
    ON short_url (original_url_hash)
    WHERE custom_alias = FALSE;
