package com.ascmoda.inventory.api.error;

public class DuplicateInventoryItemException extends RuntimeException {

    public DuplicateInventoryItemException(String message) {
        super(message);
    }
}
