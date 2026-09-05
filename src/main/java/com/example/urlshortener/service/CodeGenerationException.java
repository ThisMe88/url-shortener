package com.example.urlshortener.service;

/**
 * Thrown when a unique short code could not be generated within the configured number of
 * attempts. In practice this is astronomically unlikely (see {@link UniqueCodeGenerator});
 * it exists so the failure surfaces as a clean 500 rather than a raw constraint error.
 */
public class CodeGenerationException extends RuntimeException {

    public CodeGenerationException(String message) {
        super(message);
    }
}
