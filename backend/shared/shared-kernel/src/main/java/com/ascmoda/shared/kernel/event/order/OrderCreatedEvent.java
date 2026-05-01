package com.ascmoda.shared.kernel.event.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderCreatedEvent(
        UUID orderId,
        String orderNumber,
        UUID customerId,
        BigDecimal totalAmount,
        String currency,
        String status,
        Instant createdAt,
        String customerFullName,
        String phoneNumber,
        String externalReference,
        String paymentReference
) {
}
