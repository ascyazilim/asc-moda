package com.ascmoda.order.infrastructure.inventory;

import java.util.UUID;

public record ReleaseStockRequest(
        UUID reservationId,
        String reservationKey,
        UUID inventoryItemId,
        UUID productVariantId,
        String sku,
        Integer quantity,
        String note,
        ReferenceType referenceType,
        String referenceId
) {
}
