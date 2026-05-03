package com.ascmoda.customer.domain.exception;

public class InvalidCustomerStatusTransitionException extends RuntimeException {

    public InvalidCustomerStatusTransitionException(String message) {
        super(message);
    }
}
