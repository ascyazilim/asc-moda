package com.ascmoda.inventory.api.dto;

import com.ascmoda.inventory.domain.model.ReferenceType;
import com.ascmoda.inventory.domain.model.StockMovementType;

import java.time.Instant;
import java.util.UUID;

public record StockMovementResponse(
        UUID id,
        UUID inventoryItemId,
        UUID productVariantId,
        String sku,
        StockMovementType movementType,
        int quantity,
        String note,
        ReferenceType referenceType,
        String referenceId,
        Long version,
        Instant createdAt,
        Instant updatedAt
) {
}
