package com.ascmoda.search.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProductSearchResponse(
        UUID productId,
        String productName,
        String productSlug,
        String shortDescription,
        String categoryName,
        String categorySlug,
        String mainImageUrl,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        int variantCount,
        boolean available,
        Instant updatedAt
) {
}
