package com.ascmoda.customer.domain.exception;

public class CustomerAddressNotFoundException extends RuntimeException {

    public CustomerAddressNotFoundException(String message) {
        super(message);
    }
}
