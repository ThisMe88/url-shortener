package com.example.urlshortener.web.dto;

import java.time.Instant;

/**
 * Link analytics for a single short code.
 *
 * @param code           the short code
 * @param originalUrl    normalized destination
 * @param customAlias    true if the code was a user-supplied alias
 * @param clickCount     number of redirects served
 * @param createdAt      when the mapping was created
 * @param lastAccessedAt last redirect time, or null if never used
 */
public record StatsResponse(
        String code,
        String originalUrl,
        boolean customAlias,
        long clickCount,
        Instant createdAt,
        Instant lastAccessedAt) {
}
