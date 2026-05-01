package com.ascmoda.order.controller.dto;

import com.ascmoda.order.domain.model.OrderReservationStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderItemResponse(
        UUID id,
        UUID cartItemId,
        UUID productId,
        UUID productVariantId,
        String sku,
        String productNameSnapshot,
        String productSlugSnapshot,
        String mainImageUrlSnapshot,
        String colorSnapshot,
        String sizeSnapshot,
        BigDecimal unitPriceSnapshot,
        int quantity,
        BigDecimal lineTotal,
        UUID reservationId,
        String reservationKey,
        OrderReservationStatus reservationStatus,
        Long version,
        Instant createdAt,
        Instant updatedAt
) {
}
