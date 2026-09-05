package com.example.urlshortener.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.urlshortener.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.annotation.DirtiesContext;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ShortUrlRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private ShortUrlRepository repository;

    private static ShortUrl generated(String code, String url, String hash) {
        return ShortUrl.builder()
                .code(code)
                .originalUrl(url)
                .originalUrlHash(hash)
                .customAlias(false)
                .build();
    }

    @Test
    void persistsAndReadsBackByCode() {
        repository.save(generated("abc1234", "https://example.com/a", "hash-a"));

        ShortUrl found = repository.findByCode("abc1234").orElseThrow();

        assertThat(found.getId()).isNotNull();
        assertThat(found.getOriginalUrl()).isEqualTo("https://example.com/a");
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getClickCount()).isZero();
        assertThat(found.isCustomAlias()).isFalse();
        assertThat(found.getLastAccessedAt()).isNull();
    }

    @Test
    void findByCodeReturnsEmptyForUnknownCode() {
        assertThat(repository.findByCode("missing")).isEmpty();
    }

    @Test
    void existsByCodeReflectsPersistence() {
        assertThat(repository.existsByCode("zzz9999")).isFalse();
        repository.save(generated("zzz9999", "https://example.com/z", "hash-z"));
        assertThat(repository.existsByCode("zzz9999")).isTrue();
    }

    @Test
    void rejectsDuplicateCode() {
        repository.save(generated("dup1234", "https://example.com/1", "hash-1"));

        assertThatThrownBy(() ->
                repository.saveAndFlush(generated("dup1234", "https://example.com/2", "hash-2")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void hashLookupIgnoresCustomAliases() {
        String sharedHash = "hash-shared";
        ShortUrl alias = ShortUrl.builder()
                .code("my-alias")
                .originalUrl("https://example.com/shared")
                .originalUrlHash(sharedHash)
                .customAlias(true)
                .build();
        repository.save(alias);

        assertThat(repository.findByOriginalUrlHashAndCustomAliasFalse(sharedHash)).isEmpty();

        repository.save(generated("gen5678", "https://example.com/shared", sharedHash));

        assertThat(repository.findByOriginalUrlHashAndCustomAliasFalse(sharedHash))
                .get()
                .extracting(ShortUrl::getCode)
                .isEqualTo("gen5678");
    }

    @Test
    void partialUniqueIndexBlocksSecondGeneratedCodeForSameUrl() {
        repository.save(generated("one1234", "https://example.com/same", "hash-same"));

        assertThatThrownBy(() ->
                repository.saveAndFlush(generated("two1234", "https://example.com/same", "hash-same")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
