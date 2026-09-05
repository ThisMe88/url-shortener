package com.example.urlshortener.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShortUrlRepository extends JpaRepository<ShortUrl, Long> {

    Optional<ShortUrl> findByCode(String code);

    boolean existsByCode(String code);

    /**
     * Looks up an existing auto-generated mapping for a normalized URL, used to make
     * {@code POST /shorten} idempotent. Custom aliases are excluded so they never shadow
     * or get returned in place of a generated code.
     */
    Optional<ShortUrl> findByOriginalUrlHashAndCustomAliasFalse(String originalUrlHash);
}
