package com.example.urlshortener.domain;

import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShortUrlRepository extends JpaRepository<ShortUrl, Long> {

    Optional<ShortUrl> findByCode(String code);

    boolean existsByCode(String code);

    /**
     * Records a redirect hit in a single atomic statement so concurrent redirects of the same
     * code cannot lose increments. Bypasses the persistence context by design.
     */
    @Modifying
    @Query("""
            update ShortUrl s
               set s.clickCount = s.clickCount + 1,
                   s.lastAccessedAt = :now
             where s.code = :code
            """)
    int recordHit(@Param("code") String code, @Param("now") Instant now);

    /**
     * Looks up an existing auto-generated mapping for a normalized URL, used to make
     * {@code POST /shorten} idempotent. Custom aliases are excluded so they never shadow
     * or get returned in place of a generated code.
     */
    Optional<ShortUrl> findByOriginalUrlHashAndCustomAliasFalse(String originalUrlHash);
}
