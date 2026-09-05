package com.example.urlshortener.web.dto;

/**
 * Result of a shorten call.
 *
 * @param code        the short code (generated or the custom alias)
 * @param shortUrl    fully-qualified short link
 * @param originalUrl the normalized destination URL
 */
public record ShortenResponse(String code, String shortUrl, String originalUrl) {
}
