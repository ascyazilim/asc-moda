package com.ascmoda.order.infrastructure.cart;

import java.time.Instant;
import java.util.UUID;

public record CartResponse(
        UUID id,
        UUID customerId,
        String status,
        Instant checkedOutAt
) {
}
