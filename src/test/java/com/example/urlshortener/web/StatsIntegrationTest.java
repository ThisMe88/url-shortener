package com.example.urlshortener.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.urlshortener.AbstractIntegrationTest;
import com.example.urlshortener.domain.ShortUrlRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class StatsIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ShortUrlRepository repository;
    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void clean() {
        repository.deleteAll();
    }

    @Test
    void reportsClickCountAfterRedirects() throws Exception {
        MvcResult created = mockMvc.perform(post("/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"https://example.com/tracked\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        String code = objectMapper.readTree(created.getResponse().getContentAsString())
                .get("code").asText();

        mockMvc.perform(get("/" + code + "/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clickCount").value(0))
                .andExpect(jsonPath("$.customAlias").value(false))
                .andExpect(jsonPath("$.lastAccessedAt").doesNotExist());

        for (int i = 0; i < 2; i++) {
            mockMvc.perform(get("/" + code)).andExpect(status().isMovedPermanently());
        }

        mockMvc.perform(get("/" + code + "/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clickCount").value(2))
                .andExpect(jsonPath("$.originalUrl").value("https://example.com/tracked"))
                .andExpect(jsonPath("$.lastAccessedAt").exists());
    }

    @Test
    void unknownCodeReturns404() throws Exception {
        mockMvc.perform(get("/missing/stats")).andExpect(status().isNotFound());
    }
}
