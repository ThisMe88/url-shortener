package com.example.urlshortener.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CodeGeneratorTest {

    private static final Pattern BASE62 = Pattern.compile("[A-Za-z0-9]+");

    private final CodeGenerator generator = new CodeGenerator();

    @ParameterizedTest
    @ValueSource(ints = {4, 6, 7, 12})
    void generatesRequestedLength(int length) {
        assertThat(generator.generate(length)).hasSize(length);
    }

    @Test
    void generatesOnlyUrlSafeBase62Characters() {
        for (int i = 0; i < 5_000; i++) {
            assertThat(generator.generate(7)).matches(BASE62);
        }
    }

    @Test
    void rejectsNonPositiveLength() {
        assertThatThrownBy(() -> generator.generate(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> generator.generate(-3)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void producesEssentiallyUniqueValuesInBulk() {
        int runs = 100_000;
        Set<String> seen = new HashSet<>(runs * 2);
        for (int i = 0; i < runs; i++) {
            seen.add(generator.generate(7));
        }
        // 62^7 code space: a handful of birthday collisions is theoretically possible,
        // a broken generator would collapse the ratio far below this bound.
        assertThat((double) seen.size() / runs).isGreaterThan(0.999);
    }

    @Test
    void exercisesTheWholeAlphabet() {
        Set<Character> seen = new HashSet<>();
        for (int i = 0; i < 5_000; i++) {
            for (char c : generator.generate(7).toCharArray()) {
                seen.add(c);
            }
        }
        assertThat(seen).hasSize(CodeGenerator.ALPHABET.length());
    }
}
