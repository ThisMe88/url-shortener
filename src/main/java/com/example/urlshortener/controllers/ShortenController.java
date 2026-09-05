package com.example.urlshortener.controllers;

import com.example.urlshortener.config.AppProperties;
import com.example.urlshortener.domain.ShortUrl;
import com.example.urlshortener.service.ShortenResult;
import com.example.urlshortener.service.ShortenService;
import com.example.urlshortener.web.dto.ShortenRequest;
import com.example.urlshortener.web.dto.ShortenResponse;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ShortenController {

    private final ShortenService shortenService;
    private final AppProperties appProperties;

    public ShortenController(ShortenService shortenService, AppProperties appProperties) {
        this.shortenService = shortenService;
        this.appProperties = appProperties;
    }

    @PostMapping("/shorten")
    public ResponseEntity<ShortenResponse> shorten(@Valid @RequestBody ShortenRequest request) {
        ShortenResult result = shortenService.shorten(request.url(), request.alias());
        ShortUrl mapping = result.shortUrl();
        ShortenResponse body = new ShortenResponse(
                mapping.getCode(),
                appProperties.shortLink(mapping.getCode()),
                mapping.getOriginalUrl());

        if (result.created()) {
            return ResponseEntity
                    .created(URI.create(appProperties.shortLink(mapping.getCode())))
                    .body(body);
        }
        return ResponseEntity.ok(body);
    }
}
