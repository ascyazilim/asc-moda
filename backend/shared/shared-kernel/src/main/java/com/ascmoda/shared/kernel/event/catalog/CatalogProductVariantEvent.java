package com.ascmoda.shared.kernel.event.catalog;

import java.math.BigDecimal;
import java.util.UUID;

public record CatalogProductVariantEvent(
        UUID variantId,
        String sku,
        String color,
        String size,
        BigDecimal price,
        boolean active
) {
}
