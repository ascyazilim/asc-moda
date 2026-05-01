package com.ascmoda.search.domain.exception;

public class DuplicateEventProcessingException extends RuntimeException {

    public DuplicateEventProcessingException(String message) {
        super(message);
    }
}
