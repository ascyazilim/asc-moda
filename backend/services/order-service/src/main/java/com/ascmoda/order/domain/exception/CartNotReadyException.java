package com.ascmoda.order.domain.exception;

public class CartNotReadyException extends RuntimeException {

    public CartNotReadyException(String message) {
        super(message);
    }
}
