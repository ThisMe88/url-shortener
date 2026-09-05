package com.example.urlshortener.service;

/** The requested custom alias violates the charset/length rules or is reserved. Maps to HTTP 422. */
public class InvalidAliasException extends RuntimeException {

    public InvalidAliasException(String message) {
        super(message);
    }
}
