package com.ascmoda.catalog.api.dto;

import com.ascmoda.catalog.domain.model.ProductStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ProductResponse(
        UUID id,
        String name,
        String slug,
        String description,
        String shortDescription,
        BigDecimal basePrice,
        ProductStatus status,
        UUID categoryId,
        String categoryName,
        String categorySlug,
        List<ProductVariantResponse> variants,
        List<ProductImageResponse> images,
        Long version,
        Instant createdAt,
        Instant updatedAt
) {
}
