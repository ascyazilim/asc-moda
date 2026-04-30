package com.ascmoda.inventory.api.error;

public class InvalidStockStateException extends RuntimeException {

    public InvalidStockStateException(String message) {
        super(message);
    }
}
