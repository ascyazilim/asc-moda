package com.ascmoda.order.infrastructure.inventory;

import java.time.Instant;
import java.util.UUID;

public record ReserveStockRequest(
        UUID inventoryItemId,
        UUID productVariantId,
        String sku,
        int quantity,
        String note,
        ReferenceType referenceType,
        String referenceId,
        String reservationKey,
        Instant expiresAt
) {
}
