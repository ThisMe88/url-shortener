package com.example.urlshortener.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.urlshortener.service.RedirectService;
import com.example.urlshortener.service.ShortUrlNotFoundException;
import com.example.urlshortener.web.controllers.RedirectController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(RedirectController.class)
class RedirectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RedirectService redirectService;

    @Test
    void redirects301ToTheOriginalUrl() throws Exception {
        when(redirectService.resolveAndRecordHit("abc1234"))
                .thenReturn("https://example.com/destination");

        mockMvc.perform(get("/abc1234"))
                .andExpect(status().isMovedPermanently())
                .andExpect(header().string("Location", "https://example.com/destination"));
    }

    @Test
    void returns404ForUnknownCode() throws Exception {
        when(redirectService.resolveAndRecordHit(any()))
                .thenThrow(new ShortUrlNotFoundException("nope"));

        mockMvc.perform(get("/nope"))
                .andExpect(status().isNotFound());
    }
}
