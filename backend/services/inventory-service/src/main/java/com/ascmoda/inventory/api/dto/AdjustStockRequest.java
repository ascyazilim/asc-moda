package com.ascmoda.inventory.api.dto;

import com.ascmoda.inventory.domain.model.ReferenceType;
import com.ascmoda.inventory.domain.model.StockMovementType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record AdjustStockRequest(
        UUID inventoryItemId,
        UUID productVariantId,

        @Size(max = 120)
        String sku,

        @NotNull
        StockMovementType movementType,

        @Positive
        int quantity,

        @Size(max = 1000)
        String note,

        ReferenceType referenceType,

        @Size(max = 120)
        String referenceId
) {
}
