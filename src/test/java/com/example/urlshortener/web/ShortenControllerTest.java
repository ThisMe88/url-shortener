package com.example.urlshortener.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.urlshortener.config.AppProperties;
import com.example.urlshortener.domain.ShortUrl;
import com.example.urlshortener.service.AliasAlreadyExistsException;
import com.example.urlshortener.service.InvalidUrlException;
import com.example.urlshortener.service.ShortenResult;
import com.example.urlshortener.service.ShortenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ShortenController.class)
class ShortenControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ShortenService shortenService;

    @TestConfiguration
    static class Config {
        @Bean
        AppProperties appProperties() {
            return new AppProperties("http://localhost:8080");
        }
    }

    private static ShortUrl row(String code, String url, boolean alias) {
        return ShortUrl.builder()
                .code(code).originalUrl(url).originalUrlHash("h").customAlias(alias).build();
    }

    @Test
    void returns201WithLocationWhenANewMappingIsCreated() throws Exception {
        when(shortenService.shorten(eq("https://example.com/a"), any()))
                .thenReturn(new ShortenResult(row("abc1234", "https://example.com/a", false), true));

        mockMvc.perform(post("/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"https://example.com/a\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost:8080/abc1234"))
                .andExpect(jsonPath("$.code").value("abc1234"))
                .andExpect(jsonPath("$.shortUrl").value("http://localhost:8080/abc1234"))
                .andExpect(jsonPath("$.originalUrl").value("https://example.com/a"));
    }

    @Test
    void returns200WhenAnExistingMappingIsReturned() throws Exception {
        when(shortenService.shorten(any(), any()))
                .thenReturn(new ShortenResult(row("dup1234", "https://example.com/a", false), false));

        mockMvc.perform(post("/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"https://example.com/a\"}"))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Location"))
                .andExpect(jsonPath("$.code").value("dup1234"));
    }

    @Test
    void returns400WhenUrlIsBlank() throws Exception {
        mockMvc.perform(post("/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"  \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returns400WhenServiceRejectsTheUrl() throws Exception {
        when(shortenService.shorten(any(), any()))
                .thenThrow(new InvalidUrlException("URL must use http or https"));

        mockMvc.perform(post("/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"ftp://example.com\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("URL must use http or https"));
    }

    @Test
    void returns409WhenAliasIsTaken() throws Exception {
        when(shortenService.shorten(any(), eq("promo")))
                .thenThrow(new AliasAlreadyExistsException("promo"));

        mockMvc.perform(post("/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"https://example.com\",\"alias\":\"promo\"}"))
                .andExpect(status().isConflict());
    }
}
