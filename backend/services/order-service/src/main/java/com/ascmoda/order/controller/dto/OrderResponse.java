package com.ascmoda.order.controller.dto;

import com.ascmoda.order.domain.model.OrderSource;
import com.ascmoda.order.domain.model.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        String orderNumber,
        UUID sourceCartId,
        UUID customerId,
        OrderStatus status,
        String currency,
        BigDecimal subtotalAmount,
        BigDecimal discountAmount,
        BigDecimal shippingAmount,
        BigDecimal totalAmount,
        String note,
        OrderSource source,
        AddressSnapshotResponse shippingAddress,
        CustomerSnapshotResponse customerSnapshot,
        List<OrderItemResponse> items,
        Instant placedAt,
        Instant confirmedAt,
        Instant cancelledAt,
        String cancellationReason,
        Long version,
        Instant createdAt,
        Instant updatedAt
) {
}
