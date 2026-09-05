package com.example.urlshortener.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.urlshortener.domain.ShortUrl;
import com.example.urlshortener.domain.ShortUrlRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class ShortenServiceTest {

    @Mock
    private ShortUrlRepository repository;
    @Mock
    private UniqueCodeGenerator codeGenerator;

    private ShortenService service;

    @BeforeEach
    void setUp() {
        service = new ShortenService(
                repository, new UrlNormalizer(), new AliasPolicy(), codeGenerator);
    }

    private static ShortUrl row(String code, String url, boolean alias) {
        return ShortUrl.builder()
                .code(code).originalUrl(url).originalUrlHash("h").customAlias(alias).build();
    }

    @Test
    void generatesAndPersistsForANewUrl() {
        when(repository.findByOriginalUrlHashAndCustomAliasFalse(any())).thenReturn(Optional.empty());
        when(codeGenerator.nextCode()).thenReturn("gen1234");
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ShortenResult result = service.shorten("https://Example.com/a#x", null);

        assertThat(result.created()).isTrue();
        ArgumentCaptor<ShortUrl> saved = ArgumentCaptor.forClass(ShortUrl.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getCode()).isEqualTo("gen1234");
        assertThat(saved.getValue().getOriginalUrl()).isEqualTo("https://example.com/a");
        assertThat(saved.getValue().isCustomAlias()).isFalse();
    }

    @Test
    void returnsExistingRowForADuplicateUrlWithoutSaving() {
        ShortUrl existing = row("dup1234", "https://example.com/a", false);
        when(repository.findByOriginalUrlHashAndCustomAliasFalse(any()))
                .thenReturn(Optional.of(existing));

        ShortenResult result = service.shorten("https://example.com/a", null);

        assertThat(result.created()).isFalse();
        assertThat(result.shortUrl().getCode()).isEqualTo("dup1234");
        verify(repository, never()).save(any());
        verify(codeGenerator, never()).nextCode();
    }

    @Test
    void persistsCustomAlias() {
        when(repository.existsByCode("promo")).thenReturn(false);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ShortenResult result = service.shorten("https://example.com/landing", "promo");

        assertThat(result.created()).isTrue();
        assertThat(result.shortUrl().getCode()).isEqualTo("promo");
        assertThat(result.shortUrl().isCustomAlias()).isTrue();
    }

    @Test
    void rejectsAliasThatIsAlreadyTaken() {
        when(repository.existsByCode("taken")).thenReturn(true);

        assertThatThrownBy(() -> service.shorten("https://example.com", "taken"))
                .isInstanceOf(AliasAlreadyExistsException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void rejectsMalformedAliasBeforeTouchingTheRepository() {
        assertThatThrownBy(() -> service.shorten("https://example.com", "no"))
                .isInstanceOf(InvalidAliasException.class);
        verify(repository, never()).existsByCode(any());
    }

    @Test
    void rejectsInvalidUrl() {
        assertThatThrownBy(() -> service.shorten("ftp://example.com", null))
                .isInstanceOf(InvalidUrlException.class);
    }

    @Test
    void recoversFromAConcurrentInsertOnThePlainPath() {
        ShortUrl winner = row("winner1", "https://example.com/a", false);
        when(repository.findByOriginalUrlHashAndCustomAliasFalse(any()))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(winner));
        when(codeGenerator.nextCode()).thenReturn("loser01");
        when(repository.save(any())).thenThrow(new DataIntegrityViolationException("dup hash"));

        ShortenResult result = service.shorten("https://example.com/a", null);

        assertThat(result.created()).isFalse();
        assertThat(result.shortUrl().getCode()).isEqualTo("winner1");
    }

    @Test
    void translatesAConcurrentAliasInsertInto409() {
        when(repository.existsByCode("racy")).thenReturn(false);
        when(repository.save(any())).thenThrow(new DataIntegrityViolationException("dup code"));

        assertThatThrownBy(() -> service.shorten("https://example.com", "racy"))
                .isInstanceOf(AliasAlreadyExistsException.class);
    }
}
