package com.example.urlshortener.web;

import com.example.urlshortener.service.RedirectService;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RedirectController {

    private final RedirectService redirectService;

    public RedirectController(RedirectService redirectService) {
        this.redirectService = redirectService;
    }

    /**
     * Permanent redirect to the original URL. 301 is per the spec; note it is aggressively
     * cached by clients, so a code's destination is effectively immutable once served.
     * Unknown codes fall through to {@link GlobalExceptionHandler} as 404.
     */
    @GetMapping("/{code}")
    public ResponseEntity<Void> redirect(@PathVariable String code) {
        String target = redirectService.resolveAndRecordHit(code);
        return ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY)
                .location(URI.create(target))
                .build();
    }
}
