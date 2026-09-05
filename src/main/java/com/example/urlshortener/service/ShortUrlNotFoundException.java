package com.example.urlshortener.service;

/** No mapping exists for the requested short code. Maps to HTTP 404. */
public class ShortUrlNotFoundException extends RuntimeException {

    public ShortUrlNotFoundException(String code) {
        super("No short URL for code '" + code + "'");
    }
}
