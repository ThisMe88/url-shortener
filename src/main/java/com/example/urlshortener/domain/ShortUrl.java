package com.example.urlshortener.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A single long-URL -> short-code mapping.
 *
 * <p>{@code originalUrlHash} is the SHA-256 (hex) of the normalized URL and exists purely to
 * back the idempotent duplicate-URL lookup without indexing the full {@code TEXT} column.
 * {@code customAlias} distinguishes user-chosen codes from generated ones; the partial unique
 * index on {@code (original_url_hash) WHERE custom_alias = FALSE} enforces "one generated code
 * per URL" while leaving aliases unconstrained.
 */
@Entity
@Table(name = "short_url")
@Getter
@Setter
@NoArgsConstructor
public class ShortUrl {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 16)
    private String code;

    @Column(name = "original_url", nullable = false, columnDefinition = "text")
    private String originalUrl;

    @Column(name = "original_url_hash", nullable = false, length = 64)
    private String originalUrlHash;

    @Column(name = "custom_alias", nullable = false)
    private boolean customAlias;

    @Column(name = "click_count", nullable = false)
    private long clickCount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "last_accessed_at")
    private Instant lastAccessedAt;

    @Builder
    private ShortUrl(String code, String originalUrl, String originalUrlHash, boolean customAlias) {
        this.code = code;
        this.originalUrl = originalUrl;
        this.originalUrlHash = originalUrlHash;
        this.customAlias = customAlias;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
