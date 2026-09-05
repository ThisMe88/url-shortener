package com.example.urlshortener.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class AliasPolicyTest {

    private final AliasPolicy policy = new AliasPolicy();

    @ParameterizedTest
    @ValueSource(strings = {"abc", "my-link", "my_link", "A1b2C3", "sixteen_chars_16"})
    void acceptsWellFormedAliases(String alias) {
        assertThatCode(() -> policy.validate(alias)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "ab",                       // too short
            "seventeen_chars_17x",      // too long
            "has space",
            "has/slash",
            "emoji😀",
            "dot.dot",
    })
    void rejectsMalformedAliases(String alias) {
        assertThatThrownBy(() -> policy.validate(alias))
                .isInstanceOf(InvalidAliasException.class);
    }

    @Test
    void rejectsNull() {
        assertThatThrownBy(() -> policy.validate(null)).isInstanceOf(InvalidAliasException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"shorten", "STATS", "Actuator", "health"})
    void rejectsReservedWordsCaseInsensitively(String alias) {
        assertThatThrownBy(() -> policy.validate(alias))
                .isInstanceOf(InvalidAliasException.class)
                .hasMessageContaining("reserved");
    }
}
