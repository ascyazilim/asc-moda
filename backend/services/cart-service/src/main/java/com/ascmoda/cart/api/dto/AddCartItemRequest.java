package com.ascmoda.cart.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record AddCartItemRequest(
        @NotNull
        UUID productVariantId,

        @NotBlank
        @Size(max = 120)
        String sku,

        @Positive
        int quantity
) {
}
