package com.example.urlshortener.service;

import com.example.urlshortener.domain.ShortUrlRepository;
import com.example.urlshortener.web.dto.StatsResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read-only link analytics lookups. */
@Service
public class StatsService {

    private final ShortUrlRepository repository;

    public StatsService(ShortUrlRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public StatsResponse getStats(String code) {
        return repository.findByCode(code)
                .map(su -> new StatsResponse(
                        su.getCode(),
                        su.getOriginalUrl(),
                        su.isCustomAlias(),
                        su.getClickCount(),
                        su.getCreatedAt(),
                        su.getLastAccessedAt()))
                .orElseThrow(() -> new ShortUrlNotFoundException(code));
    }
}
