package com.ascmoda.inventory.infrastructure.catalog;

import java.util.UUID;

public record CatalogVariantResponse(
        UUID id,
        String sku,
        boolean active
) {
}
