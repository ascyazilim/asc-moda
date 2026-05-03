package com.ascmoda.customer.domain.exception;

public class NoActiveAddressException extends RuntimeException {

    public NoActiveAddressException(String message) {
        super(message);
    }
}
