package com.ascmoda.inventory.infrastructure.catalog;

import java.util.UUID;

public record CatalogVariantResponse(
        UUID id,
        UUID productId,
        String productName,
        String productSlug,
        String productStatus,
        String sku,
        String color,
        String size,
        boolean variantActive
) {
}
