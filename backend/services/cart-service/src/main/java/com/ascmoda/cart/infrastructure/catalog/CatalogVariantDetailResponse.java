package com.ascmoda.cart.infrastructure.catalog;

import java.math.BigDecimal;
import java.util.UUID;

public record CatalogVariantDetailResponse(
        UUID id,
        UUID productId,
        String productName,
        String productSlug,
        String productStatus,
        String sku,
        String color,
        String size,
        BigDecimal effectiveUnitPrice,
        String mainImageUrl,
        boolean variantActive
) {
}
