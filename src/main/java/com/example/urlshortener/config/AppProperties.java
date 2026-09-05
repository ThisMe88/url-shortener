package com.example.urlshortener.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * General application settings bound from {@code app.*}.
 *
 * @param baseUrl absolute origin used to render short links in responses,
 *                e.g. {@code https://sho.rt}; no trailing slash
 */
@Validated
@ConfigurationProperties(prefix = "app")
public record AppProperties(@NotBlank String baseUrl) {

    public String shortLink(String code) {
        String root = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        return root + "/" + code;
    }
}
