package com.example.urlshortener.config;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Tunables for short-code generation, bound from {@code app.code.*}.
 *
 * @param length                 number of characters in a generated code
 * @param maxGenerationAttempts  how many times to re-roll on a persistence
 *                               collision before giving up
 */
@Validated
@ConfigurationProperties(prefix = "app.code")
public record CodeProperties(
        @Min(4) int length,
        @Min(1) int maxGenerationAttempts) {

    public CodeProperties {
        if (length == 0) {
            length = 7;
        }
        if (maxGenerationAttempts == 0) {
            maxGenerationAttempts = 5;
        }
    }
}
