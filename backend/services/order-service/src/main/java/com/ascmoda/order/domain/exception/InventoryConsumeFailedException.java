package com.ascmoda.order.domain.exception;

public class InventoryConsumeFailedException extends RuntimeException {

    public InventoryConsumeFailedException(String message) {
        super(message);
    }
}
