package com.ascmoda.shared.kernel.event.inventory;

import java.time.Instant;
import java.util.UUID;

public record InventoryLowStockEvent(
        UUID inventoryItemId,
        UUID productVariantId,
        String sku,
        int quantityOnHand,
        int reservedQuantity,
        int availableQuantity,
        int lowStockThreshold,
        String referenceType,
        String referenceId,
        UUID reservationId,
        Instant occurredAt,
        String sourceService
) {
}
