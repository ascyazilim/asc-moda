package com.ascmoda.catalog.api.dto;

import com.ascmoda.catalog.domain.model.ProductStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductVariantDetailResponse(
        UUID id,
        UUID productId,
        String productName,
        String productSlug,
        ProductStatus productStatus,
        String sku,
        String color,
        String size,
        BigDecimal effectiveUnitPrice,
        String mainImageUrl,
        boolean variantActive
) {
}
