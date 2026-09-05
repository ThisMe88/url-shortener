package com.example.urlshortener.service;

import java.security.SecureRandom;
import org.springframework.stereotype.Component;

/**
 * Produces random, URL-safe short codes.
 *
 * <p>Codes are drawn character-by-character from a 62-symbol base62 alphabet
 * ({@code A-Z a-z 0-9}) using {@link SecureRandom}. Every character is unreserved in a URI
 * path per RFC 3986, so no percent-encoding is ever needed. The alphabet deliberately omits
 * {@code - _ ~ .} to keep codes free of separators and safe to double-click-select.
 *
 * <p>This class only mints candidates; uniqueness against the datastore is enforced by
 * {@link UniqueCodeGenerator}.
 */
@Component
public class CodeGenerator {

    static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    private final SecureRandom random = new SecureRandom();

    /** Generates a code of the given length. */
    public String generate(int length) {
        if (length < 1) {
            throw new IllegalArgumentException("length must be positive, was " + length);
        }
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}
