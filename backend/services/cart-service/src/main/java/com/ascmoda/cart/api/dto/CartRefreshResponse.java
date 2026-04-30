package com.ascmoda.cart.api.dto;

public record CartRefreshResponse(
        CartResponse cart,
        CartValidationResponse validation
) {
}
