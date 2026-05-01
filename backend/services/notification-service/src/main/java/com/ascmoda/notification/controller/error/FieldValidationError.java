package com.ascmoda.notification.controller.error;

public record FieldValidationError(
        String field,
        String message
) {
}
