package com.example.urlshortener.service;

/** The submitted URL is missing, malformed, or uses an unsupported scheme. Maps to HTTP 400. */
public class InvalidUrlException extends RuntimeException {

    public InvalidUrlException(String message) {
        super(message);
    }
}
