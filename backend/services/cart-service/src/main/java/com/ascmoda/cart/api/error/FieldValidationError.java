package com.ascmoda.cart.api.error;

public record FieldValidationError(
        String field,
        String message
) {
}
