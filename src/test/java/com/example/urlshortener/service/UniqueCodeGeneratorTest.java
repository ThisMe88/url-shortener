package com.example.urlshortener.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.urlshortener.config.CodeProperties;
import com.example.urlshortener.domain.ShortUrlRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UniqueCodeGeneratorTest {

    @Mock
    private CodeGenerator codeGenerator;

    @Mock
    private ShortUrlRepository repository;

    private UniqueCodeGenerator uniqueCodeGenerator;

    @BeforeEach
    void setUp() {
        uniqueCodeGenerator = new UniqueCodeGenerator(
                codeGenerator, repository, new CodeProperties(7, 5));
    }

    @Test
    void returnsFirstCandidateWhenNotTaken() {
        when(codeGenerator.generate(7)).thenReturn("aaaaaaa");
        when(repository.existsByCode("aaaaaaa")).thenReturn(false);

        assertThat(uniqueCodeGenerator.nextCode()).isEqualTo("aaaaaaa");
        verify(codeGenerator, times(1)).generate(7);
    }

    @Test
    void retriesUntilItFindsAFreeCode() {
        when(codeGenerator.generate(7)).thenReturn("taken01", "taken02", "free123");
        when(repository.existsByCode("taken01")).thenReturn(true);
        when(repository.existsByCode("taken02")).thenReturn(true);
        when(repository.existsByCode("free123")).thenReturn(false);

        assertThat(uniqueCodeGenerator.nextCode()).isEqualTo("free123");
        verify(codeGenerator, times(3)).generate(7);
    }

    @Test
    void givesUpAfterMaxAttempts() {
        when(codeGenerator.generate(anyInt())).thenReturn("collide");
        when(repository.existsByCode("collide")).thenReturn(true);

        assertThatThrownBy(() -> uniqueCodeGenerator.nextCode())
                .isInstanceOf(CodeGenerationException.class)
                .hasMessageContaining("5 attempts");
        verify(codeGenerator, times(5)).generate(7);
    }

    @Test
    void honoursConfiguredCodeLength() {
        uniqueCodeGenerator = new UniqueCodeGenerator(
                codeGenerator, repository, new CodeProperties(10, 3));
        when(codeGenerator.generate(10)).thenReturn("0123456789");
        when(repository.existsByCode("0123456789")).thenReturn(false);

        assertThat(uniqueCodeGenerator.nextCode()).hasSize(10);
        verify(codeGenerator).generate(10);
    }
}
