package com.ascmoda.order.infrastructure.cart;

import java.util.UUID;

public record CartValidationIssueResponse(
        UUID itemId,
        UUID productVariantId,
        String sku,
        String type,
        String message
) {
}
