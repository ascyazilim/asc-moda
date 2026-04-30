package com.ascmoda.inventory.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record UpdateInventoryItemRequest(
        @NotNull
        UUID productVariantId,

        @NotBlank
        @Size(max = 120)
        String sku,

        @PositiveOrZero
        int quantityOnHand,

        @PositiveOrZero
        int reservedQuantity,

        Boolean active,

        @PositiveOrZero
        Integer lowStockThreshold
) {
}
