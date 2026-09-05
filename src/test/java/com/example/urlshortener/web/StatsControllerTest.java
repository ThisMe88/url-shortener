package com.example.urlshortener.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.urlshortener.service.ShortUrlNotFoundException;
import com.example.urlshortener.service.StatsService;
import com.example.urlshortener.web.controllers.StatsController;
import com.example.urlshortener.web.dto.StatsResponse;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(StatsController.class)
class StatsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StatsService statsService;

    @Test
    void returnsStatsBody() throws Exception {
        when(statsService.getStats("abc1234")).thenReturn(new StatsResponse(
                "abc1234", "https://example.com/a", false, 5,
                Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-01-02T00:00:00Z")));

        mockMvc.perform(get("/abc1234/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("abc1234"))
                .andExpect(jsonPath("$.originalUrl").value("https://example.com/a"))
                .andExpect(jsonPath("$.customAlias").value(false))
                .andExpect(jsonPath("$.clickCount").value(5))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.lastAccessedAt").exists());
    }

    @Test
    void returns404ForUnknownCode() throws Exception {
        when(statsService.getStats(any())).thenThrow(new ShortUrlNotFoundException("nope"));

        mockMvc.perform(get("/nope/stats")).andExpect(status().isNotFound());
    }
}
