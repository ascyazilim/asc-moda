package com.ascmoda.cart.infrastructure.inventory;

import java.util.UUID;

public record InventoryItemResponse(
        UUID id,
        UUID productVariantId,
        String sku,
        int quantityOnHand,
        int reservedQuantity,
        int availableQuantity,
        boolean active
) {
}
