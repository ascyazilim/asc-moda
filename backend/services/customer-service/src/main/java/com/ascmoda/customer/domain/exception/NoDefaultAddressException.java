package com.ascmoda.customer.domain.exception;

public class NoDefaultAddressException extends RuntimeException {

    public NoDefaultAddressException(String message) {
        super(message);
    }
}
