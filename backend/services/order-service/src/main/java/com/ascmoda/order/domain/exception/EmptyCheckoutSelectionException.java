package com.ascmoda.order.domain.exception;

public class EmptyCheckoutSelectionException extends RuntimeException {

    public EmptyCheckoutSelectionException(String message) {
        super(message);
    }
}
