package com.ascmoda.order.infrastructure.inventory;

import java.util.UUID;

public record ConsumeStockReservationRequest(
        UUID reservationId,
        String reservationKey,
        Integer quantity,
        String note
) {
}
