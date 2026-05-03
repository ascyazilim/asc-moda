package com.ascmoda.customer.domain.exception;

public class DuplicateExternalUserIdException extends RuntimeException {

    public DuplicateExternalUserIdException(String message) {
        super(message);
    }
}
