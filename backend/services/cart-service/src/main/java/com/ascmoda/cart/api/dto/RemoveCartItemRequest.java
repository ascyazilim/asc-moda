package com.ascmoda.cart.api.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record RemoveCartItemRequest(
        @NotNull
        UUID itemId
) {
}
