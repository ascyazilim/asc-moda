package com.ascmoda.order.domain.exception;

public class DuplicateOrderAttemptException extends RuntimeException {

    public DuplicateOrderAttemptException(String message) {
        super(message);
    }
}
