package com.ascmoda.order.domain.exception;

public class InventoryReservationFailedException extends RuntimeException {

    public InventoryReservationFailedException(String message) {
        super(message);
    }
}
