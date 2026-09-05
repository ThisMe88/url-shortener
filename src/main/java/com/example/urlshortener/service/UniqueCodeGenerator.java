package com.example.urlshortener.service;

import com.example.urlshortener.config.CodeProperties;
import com.example.urlshortener.domain.ShortUrlRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Returns a short code that is not already present in the datastore.
 *
 * <h2>Why collisions are a non-issue</h2>
 * With a 62-character alphabet and the default length of 7, the code space is
 * 62^7 &#8776; 3.52 &#215; 10^12. The probability that a single freshly generated code
 * collides with an existing row is {@code n / 62^7}, where {@code n} is the number of codes
 * already stored. Even at 10 million stored URLs that is ~2.8 &#215; 10^-6 per attempt, and
 * the attempts are independent, so the chance of {@code maxGenerationAttempts} (default 5)
 * consecutive collisions is ~1.8 &#215; 10^-28. The explicit {@code existsByCode} pre-check
 * plus the {@code ux_short_url_code} unique constraint (which makes the final INSERT the real
 * arbiter under concurrency) mean a collision can never produce a duplicate — at worst it
 * costs one extra round trip.
 */
@Component
public class UniqueCodeGenerator {

    private static final Logger log = LoggerFactory.getLogger(UniqueCodeGenerator.class);

    private final CodeGenerator codeGenerator;
    private final ShortUrlRepository repository;
    private final CodeProperties properties;

    public UniqueCodeGenerator(
            CodeGenerator codeGenerator,
            ShortUrlRepository repository,
            CodeProperties properties) {
        this.codeGenerator = codeGenerator;
        this.repository = repository;
        this.properties = properties;
    }

    public String nextCode() {
        int maxAttempts = properties.maxGenerationAttempts();
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            String candidate = codeGenerator.generate(properties.length());
            if (!repository.existsByCode(candidate)) {
                return candidate;
            }
            log.warn("Short-code collision on attempt {}/{} for candidate {}",
                    attempt, maxAttempts, candidate);
        }
        throw new CodeGenerationException(
                "Could not generate a unique short code after " + maxAttempts + " attempts");
    }
}
