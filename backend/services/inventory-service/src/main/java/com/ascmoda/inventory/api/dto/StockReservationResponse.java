package com.ascmoda.inventory.api.dto;

import com.ascmoda.inventory.domain.model.ReferenceType;
import com.ascmoda.inventory.domain.model.StockReservationStatus;

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
