package com.ascmoda.inventory.api.dto;

import java.util.UUID;

public record AvailabilityResponse(
        UUID inventoryItemId,
        UUID productVariantId,
        String sku,
        int quantityOnHand,
        int reservedQuantity,
        int availableQuantity,
        int requestedQuantity,
        boolean active,
        boolean lowStock,
        boolean available
) {
}
