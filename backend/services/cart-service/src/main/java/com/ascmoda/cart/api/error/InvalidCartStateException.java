package com.ascmoda.cart.api.error;

public class InvalidCartStateException extends RuntimeException {

    public InvalidCartStateException(String message) {
        super(message);
    }
}
