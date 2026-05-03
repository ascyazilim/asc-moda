package com.ascmoda.customer.domain.exception;

public class BlockedCustomerOperationException extends RuntimeException {

    public BlockedCustomerOperationException(String message) {
        super(message);
    }
}
