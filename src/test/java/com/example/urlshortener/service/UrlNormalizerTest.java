package com.example.urlshortener.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class UrlNormalizerTest {

    private final UrlNormalizer normalizer = new UrlNormalizer();

    @ParameterizedTest
    @CsvSource({
            "https://example.com,                     https://example.com",
            "HTTPS://Example.COM/Path,                https://example.com/Path",
            "http://example.com:80/a,                 http://example.com/a",
            "https://example.com:443,                 https://example.com",
            "https://example.com:8443/x,              https://example.com:8443/x",
            "https://example.com/p?b=2&a=1,           https://example.com/p?b=2&a=1",
            "https://example.com/p#frag,              https://example.com/p",
            "  https://example.com/trim  ,            https://example.com/trim",
    })
    void normalizesCanonically(String input, String expected) {
        assertThat(normalizer.normalize(input)).isEqualTo(expected);
    }

    @Test
    void preservesQueryOrderAndCasePath() {
        assertThat(normalizer.normalize("https://EXAMPLE.com/Ab/Cd?Q=Zz"))
                .isEqualTo("https://example.com/Ab/Cd?Q=Zz");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "ftp://example.com/file",
            "example.com/no-scheme",
            "https://",
            "not a url",
            "mailto:someone@example.com",
    })
    void rejectsUnsupportedOrMalformed(String input) {
        assertThatThrownBy(() -> normalizer.normalize(input))
                .isInstanceOf(InvalidUrlException.class);
    }

    @Test
    void rejectsBlank() {
        assertThatThrownBy(() -> normalizer.normalize("  "))
                .isInstanceOf(InvalidUrlException.class);
        assertThatThrownBy(() -> normalizer.normalize(null))
                .isInstanceOf(InvalidUrlException.class);
    }

    @Test
    void rejectsOverlyLongUrl() {
        String longUrl = "https://example.com/" + "x".repeat(2100);
        assertThatThrownBy(() -> normalizer.normalize(longUrl))
                .isInstanceOf(InvalidUrlException.class)
                .hasMessageContaining("maximum length");
    }
}
