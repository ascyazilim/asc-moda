package com.ascmoda.inventory.api.dto;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record ValidateStockRequest(
        UUID productVariantId,

        @Size(max = 120)
        String sku,

        @Positive
        int quantity
) {
}
