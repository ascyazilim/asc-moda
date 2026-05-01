package com.ascmoda.order.infrastructure.inventory;

import java.time.Instant;
import java.util.UUID;

public record StockReservationResponse(
        UUID id,
        UUID inventoryItemId,
        UUID productVariantId,
        String sku,
        ReferenceType referenceType,
        String referenceId,
        String reservationKey,
        int quantity,
        StockReservationStatus status,
        Instant expiresAt,
        Long version,
        Instant createdAt,
        Instant updatedAt
) {
}
