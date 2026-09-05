package com.example.urlshortener.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.urlshortener.AbstractIntegrationTest;
import com.example.urlshortener.domain.ShortUrl;
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
class RedirectIntegrationTest extends AbstractIntegrationTest {

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

    private String shortenAndGetCode(String url) throws Exception {
        MvcResult result = mockMvc.perform(post("/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"" + url + "\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("code").asText();
    }

    @Test
    void redirectsToTheOriginalUrlWith301() throws Exception {
        String code = shortenAndGetCode("https://example.com/target");

        mockMvc.perform(get("/" + code))
                .andExpect(status().isMovedPermanently())
                .andExpect(header().string("Location", "https://example.com/target"));
    }

    @Test
    void unknownCodeReturns404() throws Exception {
        mockMvc.perform(get("/doesnotexist")).andExpect(status().isNotFound());
    }

    @Test
    void eachRedirectIncrementsClickCountAndStampsLastAccessed() throws Exception {
        String code = shortenAndGetCode("https://example.com/counted");

        for (int i = 0; i < 3; i++) {
            mockMvc.perform(get("/" + code)).andExpect(status().isMovedPermanently());
        }

        ShortUrl row = repository.findByCode(code).orElseThrow();
        assertThat(row.getClickCount()).isEqualTo(3);
        assertThat(row.getLastAccessedAt()).isNotNull();
    }
}
