package com.example.urlshortener.service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Validates an incoming URL and reduces it to a canonical form.
 *
 * <p>Rules enforced:
 * <ul>
 *   <li>non-blank and at most {@value #MAX_LENGTH} characters</li>
 *   <li>parses as an absolute {@link URI} with a scheme of {@code http} or {@code https}</li>
 *   <li>has a host</li>
 * </ul>
 *
 * <p>Normalization (so that trivially different spellings of the same target dedupe together):
 * lower-cases the scheme and host, drops a default port (80/443), and strips any fragment.
 * Path and query are preserved verbatim &mdash; they can be semantically significant and we
 * do not want to over-normalize and collapse genuinely different links.
 */
@Component
public class UrlNormalizer {

    static final int MAX_LENGTH = 2048;
    private static final Set<String> ALLOWED_SCHEMES = Set.of("http", "https");

    public String normalize(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            throw new InvalidUrlException("URL must not be blank");
        }
        String trimmed = rawUrl.trim();
        if (trimmed.length() > MAX_LENGTH) {
            throw new InvalidUrlException("URL exceeds maximum length of " + MAX_LENGTH);
        }

        URI uri;
        try {
            uri = new URI(trimmed);
        } catch (URISyntaxException e) {
            throw new InvalidUrlException("URL is malformed: " + e.getReason());
        }

        String scheme = uri.getScheme();
        if (scheme == null || !ALLOWED_SCHEMES.contains(scheme.toLowerCase(Locale.ROOT))) {
            throw new InvalidUrlException("URL must use http or https");
        }
        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new InvalidUrlException("URL must include a host");
        }

        scheme = scheme.toLowerCase(Locale.ROOT);
        host = host.toLowerCase(Locale.ROOT);

        int port = uri.getPort();
        boolean defaultPort = port == -1
                || (scheme.equals("http") && port == 80)
                || (scheme.equals("https") && port == 443);

        StringBuilder sb = new StringBuilder(scheme).append("://").append(host);
        if (!defaultPort) {
            sb.append(':').append(port);
        }
        if (uri.getRawPath() != null) {
            sb.append(uri.getRawPath());
        }
        if (uri.getRawQuery() != null) {
            sb.append('?').append(uri.getRawQuery());
        }
        return sb.toString();
    }
}
