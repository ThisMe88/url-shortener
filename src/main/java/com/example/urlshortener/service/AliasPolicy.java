package com.example.urlshortener.service;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Rules for user-supplied custom aliases.
 *
 * <p>Allowed: 3&ndash;16 characters of {@code [A-Za-z0-9_-]}. Reserved words that collide with
 * existing or foreseeable routes are rejected so an alias can never shadow an API path.
 */
@Component
public class AliasPolicy {

    private static final Pattern VALID = Pattern.compile("^[A-Za-z0-9_-]{3,16}$");
    private static final Set<String> RESERVED = Set.of(
            "shorten", "stats", "actuator", "health", "info", "api", "admin");

    public void validate(String alias) {
        if (alias == null || !VALID.matcher(alias).matches()) {
            throw new InvalidAliasException(
                    "Alias must be 3-16 characters of letters, digits, hyphen or underscore");
        }
        if (RESERVED.contains(alias.toLowerCase(Locale.ROOT))) {
            throw new InvalidAliasException("Alias '" + alias + "' is reserved");
        }
    }
}
