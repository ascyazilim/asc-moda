package com.ascmoda.cart.api.dto;

import java.util.UUID;

public record CartValidationIssueResponse(
        UUID itemId,
        UUID productVariantId,
        String sku,
        CartValidationIssueType type,
        String message
) {
}
