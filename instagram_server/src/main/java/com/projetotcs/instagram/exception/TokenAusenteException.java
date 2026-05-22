package com.projetotcs.instagram.exception;

public class TokenAusenteException extends RuntimeException {
    public TokenAusenteException(String message) {
        super(message);
    }
}
