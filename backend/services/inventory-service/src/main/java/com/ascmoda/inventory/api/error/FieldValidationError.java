package com.ascmoda.inventory.api.error;

public record FieldValidationError(
        String field,
        String message
) {
}
