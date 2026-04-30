package com.ascmoda.cart.api.dto;

import jakarta.validation.constraints.Positive;

public record UpdateCartItemQuantityRequest(
        @Positive
        int quantity
) {
}
