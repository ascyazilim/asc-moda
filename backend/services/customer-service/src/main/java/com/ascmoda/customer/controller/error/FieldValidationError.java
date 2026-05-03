package com.ascmoda.customer.controller.error;

public record FieldValidationError(
        String field,
        String message
) {
}
