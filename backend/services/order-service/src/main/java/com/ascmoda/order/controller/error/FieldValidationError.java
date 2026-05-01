package com.ascmoda.order.controller.error;

public record FieldValidationError(
        String field,
        String message
) {
}
