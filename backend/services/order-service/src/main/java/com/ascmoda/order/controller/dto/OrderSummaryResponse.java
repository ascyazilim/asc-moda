package com.ascmoda.order.controller.dto;

import com.ascmoda.order.domain.model.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderSummaryResponse(
        UUID id,
        String orderNumber,
        UUID customerId,
        OrderStatus status,
        String currency,
        BigDecimal totalAmount,
        int itemCount,
        Instant placedAt,
        Instant confirmedAt,
        Instant cancelledAt
) {
}
