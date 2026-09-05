package com.example.urlshortener.service;

import com.example.urlshortener.domain.ShortUrl;
import com.example.urlshortener.domain.ShortUrlRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/**
 * Creates short-code mappings.
 *
 * <h2>Duplicate-URL policy</h2>
 * A plain shorten (no alias) is <strong>idempotent</strong>: the same normalized URL always
 * resolves to the same generated code. The caller gets 201 the first time and 200 on repeats.
 * Custom aliases are exempt &mdash; you can always mint an alias for a URL that already has a
 * generated code, and each alias is its own row.
 *
 * <h2>Concurrency</h2>
 * The method is intentionally not wrapped in a single transaction. Each repository call runs
 * in its own transaction and the database constraints are the source of truth:
 * <ul>
 *   <li>plain path: if two requests race to shorten the same new URL, the partial unique index
 *       {@code ux_short_url_hash_generated} rejects the loser's insert; we catch it and return
 *       the winner's row (still 200, still idempotent).</li>
 *   <li>alias path: a race on the same alias trips {@code ux_short_url_code}; we translate that
 *       into {@link AliasAlreadyExistsException} (409).</li>
 * </ul>
 */
@Service
public class ShortenService {

    private static final Logger log = LoggerFactory.getLogger(ShortenService.class);

    private final ShortUrlRepository repository;
    private final UrlNormalizer urlNormalizer;
    private final AliasPolicy aliasPolicy;
    private final UniqueCodeGenerator codeGenerator;

    public ShortenService(
            ShortUrlRepository repository,
            UrlNormalizer urlNormalizer,
            AliasPolicy aliasPolicy,
            UniqueCodeGenerator codeGenerator) {
        this.repository = repository;
        this.urlNormalizer = urlNormalizer;
        this.aliasPolicy = aliasPolicy;
        this.codeGenerator = codeGenerator;
    }

    public ShortenResult shorten(String rawUrl, String alias) {
        String normalizedUrl = urlNormalizer.normalize(rawUrl);
        String hash = Hashing.sha256Hex(normalizedUrl);

        if (alias != null && !alias.isBlank()) {
            return createWithAlias(alias.trim(), normalizedUrl, hash);
        }
        return createWithGeneratedCode(normalizedUrl, hash);
    }

    private ShortenResult createWithAlias(String alias, String normalizedUrl, String hash) {
        aliasPolicy.validate(alias);
        if (repository.existsByCode(alias)) {
            throw new AliasAlreadyExistsException(alias);
        }
        ShortUrl entity = ShortUrl.builder()
                .code(alias)
                .originalUrl(normalizedUrl)
                .originalUrlHash(hash)
                .customAlias(true)
                .build();
        try {
            return new ShortenResult(repository.save(entity), true);
        } catch (DataIntegrityViolationException e) {
            // Lost a race for the same alias between the check above and the insert.
            throw new AliasAlreadyExistsException(alias);
        }
    }

    private ShortenResult createWithGeneratedCode(String normalizedUrl, String hash) {
        var existing = repository.findByOriginalUrlHashAndCustomAliasFalse(hash);
        if (existing.isPresent()) {
            return new ShortenResult(existing.get(), false);
        }
        ShortUrl entity = ShortUrl.builder()
                .code(codeGenerator.nextCode())
                .originalUrl(normalizedUrl)
                .originalUrlHash(hash)
                .customAlias(false)
                .build();
        try {
            return new ShortenResult(repository.save(entity), true);
        } catch (DataIntegrityViolationException e) {
            // Another request shortened the same URL first; return its row (still idempotent).
            log.debug("Concurrent shorten for hash {}, returning existing row", hash);
            ShortUrl winner = repository.findByOriginalUrlHashAndCustomAliasFalse(hash)
                    .orElseThrow(() -> e);
            return new ShortenResult(winner, false);
        }
    }
}
