package com.ascmoda.notification.domain.exception;

public class InvalidMessagePayloadException extends RuntimeException {

    public InvalidMessagePayloadException(String message) {
        super(message);
    }
}
