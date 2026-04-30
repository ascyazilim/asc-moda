package com.ascmoda.inventory.api.dto;

public record InventorySummaryResponse(
        long itemCount,
        long activeItemCount,
        long inactiveItemCount,
        long lowStockItemCount,
        long totalQuantityOnHand,
        long totalReservedQuantity,
        long totalAvailableQuantity
) {
}
