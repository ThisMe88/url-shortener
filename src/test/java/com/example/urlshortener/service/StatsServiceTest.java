package com.example.urlshortener.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.example.urlshortener.domain.ShortUrl;
import com.example.urlshortener.domain.ShortUrlRepository;
import com.example.urlshortener.web.dto.StatsResponse;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StatsServiceTest {

    @Mock
    private ShortUrlRepository repository;

    @Test
    void mapsPersistedFieldsIntoTheResponse() {
        ShortUrl row = ShortUrl.builder()
                .code("abc1234").originalUrl("https://example.com/a").originalUrlHash("h")
                .customAlias(true).build();
        row.setClickCount(9);
        when(repository.findByCode("abc1234")).thenReturn(Optional.of(row));

        StatsResponse stats = new StatsService(repository).getStats("abc1234");

        assertThat(stats.code()).isEqualTo("abc1234");
        assertThat(stats.originalUrl()).isEqualTo("https://example.com/a");
        assertThat(stats.customAlias()).isTrue();
        assertThat(stats.clickCount()).isEqualTo(9);
    }

    @Test
    void throwsNotFoundForUnknownCode() {
        when(repository.findByCode("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> new StatsService(repository).getStats("missing"))
                .isInstanceOf(ShortUrlNotFoundException.class);
    }
}
