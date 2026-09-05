package com.example.urlshortener.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.urlshortener.domain.ShortUrl;
import com.example.urlshortener.domain.ShortUrlRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RedirectServiceTest {

    @Mock
    private ShortUrlRepository repository;

    private RedirectService service() {
        return new RedirectService(repository);
    }

    @Test
    void resolvesDestinationAndRecordsHit() {
        ShortUrl row = ShortUrl.builder()
                .code("abc1234").originalUrl("https://example.com/a").originalUrlHash("h")
                .customAlias(false).build();
        when(repository.findByCode("abc1234")).thenReturn(Optional.of(row));

        String target = service().resolveAndRecordHit("abc1234");

        assertThat(target).isEqualTo("https://example.com/a");
        verify(repository).recordHit(eq("abc1234"), any());
    }

    @Test
    void throwsAndDoesNotRecordHitForUnknownCode() {
        when(repository.findByCode("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().resolveAndRecordHit("missing"))
                .isInstanceOf(ShortUrlNotFoundException.class);
        verify(repository, never()).recordHit(any(), any());
    }
}
