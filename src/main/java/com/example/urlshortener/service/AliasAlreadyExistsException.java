package com.example.urlshortener.service;

/** The requested custom alias is already taken by another mapping. Maps to HTTP 409. */
public class AliasAlreadyExistsException extends RuntimeException {

    public AliasAlreadyExistsException(String alias) {
        super("Alias '" + alias + "' is already in use");
    }
}
