package com.ascmoda.catalog.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProductVariantResponse(
        UUID id,
        String sku,
        String color,
        String size,
        String stockKeepingNote,
        BigDecimal priceOverride,
        boolean active,
        Long version,
        Instant createdAt,
        Instant updatedAt
) {
}
