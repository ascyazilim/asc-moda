package com.ascmoda.catalog.api.error;

public record FieldValidationError(
        String field,
        String message
) {
}
