package com.example.urlshortener.service;

import com.example.urlshortener.domain.ShortUrlRepository;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Resolves a short code to its destination and records the hit. */
@Service
public class RedirectService {

    private final ShortUrlRepository repository;

    public RedirectService(ShortUrlRepository repository) {
        this.repository = repository;
    }

    /**
     * @return the destination URL for {@code code}
     * @throws ShortUrlNotFoundException if the code is unknown
     */
    @Transactional
    public String resolveAndRecordHit(String code) {
        String target = repository.findByCode(code)
                .orElseThrow(() -> new ShortUrlNotFoundException(code))
                .getOriginalUrl();
        repository.recordHit(code, Instant.now());
        return target;
    }
}
