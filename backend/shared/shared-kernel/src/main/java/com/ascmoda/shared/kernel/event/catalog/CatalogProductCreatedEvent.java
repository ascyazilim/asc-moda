package com.ascmoda.shared.kernel.event.catalog;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CatalogProductCreatedEvent(
        UUID productId,
        String productName,
        String productSlug,
        String shortDescription,
        String description,
        UUID categoryId,
        String categoryName,
        String categorySlug,
        String status,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        List<CatalogProductVariantEvent> variants,
        String mainImageUrl,
        String searchableText,
        Instant occurredAt,
        String sourceService
) {
}
