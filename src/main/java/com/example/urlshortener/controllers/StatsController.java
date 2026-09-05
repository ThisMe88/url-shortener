package com.example.urlshortener.controllers;

import com.example.urlshortener.service.StatsService;
import com.example.urlshortener.web.dto.StatsResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StatsController {

    private final StatsService statsService;

    public StatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    /** Link analytics for a code. 404 (via {@link com.example.urlshortener.web.GlobalExceptionHandler}) if the code is unknown. */
    @GetMapping("/{code}/stats")
    public StatsResponse stats(@PathVariable String code) {
        return statsService.getStats(code);
    }
}
