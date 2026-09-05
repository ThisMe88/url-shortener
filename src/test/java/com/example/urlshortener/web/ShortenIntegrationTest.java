package com.example.urlshortener.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.urlshortener.AbstractIntegrationTest;
import com.example.urlshortener.domain.ShortUrlRepository;
import com.fasterxml.jackson.databind.JsonNode;
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
class ShortenIntegrationTest extends AbstractIntegrationTest {

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

    private JsonNode shorten(String json, int expectedStatus) throws Exception {
        MvcResult result = mockMvc.perform(post("/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().is(expectedStatus))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    @Test
    void shortensANewUrlAndReturns201() throws Exception {
        JsonNode body = shorten("{\"url\":\"https://example.com/docs?a=1#top\"}", 201);

        assertThat(body.get("code").asText()).hasSize(7);
        assertThat(body.get("originalUrl").asText()).isEqualTo("https://example.com/docs?a=1");
        assertThat(body.get("shortUrl").asText()).endsWith("/" + body.get("code").asText());
        assertThat(repository.findByCode(body.get("code").asText())).isPresent();
    }

    @Test
    void repeatingTheSameUrlIsIdempotentAndReturns200() throws Exception {
        JsonNode first = shorten("{\"url\":\"https://example.com/same\"}", 201);
        JsonNode second = shorten("{\"url\":\"https://example.com/same\"}", 200);

        assertThat(second.get("code").asText()).isEqualTo(first.get("code").asText());
        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    void honoursACustomAlias() throws Exception {
        JsonNode body = shorten(
                "{\"url\":\"https://example.com/landing\",\"alias\":\"launch-2026\"}", 201);

        assertThat(body.get("code").asText()).isEqualTo("launch-2026");
        assertThat(repository.findByCode("launch-2026")).get()
                .extracting(su -> su.isCustomAlias()).isEqualTo(true);
    }

    @Test
    void rejectsADuplicateAliasWith409() throws Exception {
        shorten("{\"url\":\"https://example.com/a\",\"alias\":\"dup\"}", 201);
        shorten("{\"url\":\"https://example.com/b\",\"alias\":\"dup\"}", 409);
    }

    @Test
    void rejectsAReservedAliasWith422() throws Exception {
        shorten("{\"url\":\"https://example.com/a\",\"alias\":\"stats\"}", 422);
    }

    @Test
    void rejectsAnInvalidUrlWith400() throws Exception {
        shorten("{\"url\":\"not-a-url\"}", 400);
    }

    @Test
    void aliasAndGeneratedCodeCanCoexistForTheSameUrl() throws Exception {
        JsonNode generated = shorten("{\"url\":\"https://example.com/x\"}", 201);
        JsonNode aliased = shorten(
                "{\"url\":\"https://example.com/x\",\"alias\":\"ex-x\"}", 201);

        assertThat(aliased.get("code").asText()).isEqualTo("ex-x");
        assertThat(generated.get("code").asText()).isNotEqualTo("ex-x");
        assertThat(repository.count()).isEqualTo(2);
    }
}
