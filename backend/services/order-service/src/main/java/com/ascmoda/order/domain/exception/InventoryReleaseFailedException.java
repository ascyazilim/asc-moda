package com.ascmoda.order.domain.exception;

public class InventoryReleaseFailedException extends RuntimeException {

    public InventoryReleaseFailedException(String message) {
        super(message);
    }
}
