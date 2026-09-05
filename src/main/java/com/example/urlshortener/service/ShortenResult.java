package com.example.urlshortener.service;

import com.example.urlshortener.domain.ShortUrl;

/**
 * Outcome of a shorten call.
 *
 * @param shortUrl the persisted (or pre-existing) mapping
 * @param created  {@code true} if a new row was inserted, {@code false} if an existing
 *                 mapping was returned (idempotent duplicate) &mdash; lets the controller
 *                 choose 201 vs 200
 */
public record ShortenResult(ShortUrl shortUrl, boolean created) {
}
