package com.ascmoda.inventory.api.dto;

import java.time.Instant;
import java.util.UUID;

public record InventoryItemResponse(
        UUID id,
        UUID productVariantId,
        String sku,
        int quantityOnHand,
        int reservedQuantity,
        int availableQuantity,
        boolean active,
        Long version,
        Instant createdAt,
        Instant updatedAt
) {
}
