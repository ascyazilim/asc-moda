package com.ascmoda.catalog.api.dto;

import java.time.Instant;
import java.util.UUID;

public record ProductImageResponse(
        UUID id,
        UUID productId,
        UUID variantId,
        String imageUrl,
        String altText,
        int sortOrder,
        boolean main,
        boolean active,
        Long version,
        Instant createdAt,
        Instant updatedAt
) {
}
