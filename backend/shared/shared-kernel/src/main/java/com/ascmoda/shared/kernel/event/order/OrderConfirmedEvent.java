package com.ascmoda.shared.kernel.event.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderConfirmedEvent(
        UUID orderId,
        String orderNumber,
        UUID customerId,
        BigDecimal totalAmount,
        String currency,
        String status,
        Instant confirmedAt,
        String customerFullName,
        String phoneNumber,
        String externalReference,
        String paymentReference
) {
}
