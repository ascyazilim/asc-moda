package com.ascmoda.order.controller.dto;

import com.ascmoda.order.domain.model.OrderSource;
import com.ascmoda.order.domain.model.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderListItemResponse(
        UUID id,
        String orderNumber,
        UUID sourceCartId,
        UUID customerId,
        String externalReference,
        String paymentReference,
        OrderStatus status,
        OrderSource source,
        String currency,
        BigDecimal totalAmount,
        int itemCount,
        Instant placedAt,
        Instant createdAt
) {
}
