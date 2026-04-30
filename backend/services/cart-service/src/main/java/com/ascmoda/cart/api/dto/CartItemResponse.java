package com.ascmoda.cart.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CartItemResponse(
        UUID id,
        UUID productId,
        UUID productVariantId,
        String sku,
        String productNameSnapshot,
        String productSlugSnapshot,
        String variantNameSnapshot,
        String mainImageUrlSnapshot,
        String colorSnapshot,
        String sizeSnapshot,
        BigDecimal unitPriceSnapshot,
        int quantity,
        boolean selected,
        BigDecimal lineTotal,
        Long version,
        Instant createdAt,
        Instant updatedAt
) {
}
