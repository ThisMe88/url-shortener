package com.example.urlshortener.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Body of {@code POST /shorten}.
 *
 * @param url   the long URL to shorten (required)
 * @param alias optional custom alias to use instead of a generated code
 */
public record ShortenRequest(
        @NotBlank(message = "url is required")
        @Size(max = 2048, message = "url exceeds maximum length of 2048")
        String url,

        String alias) {
}
