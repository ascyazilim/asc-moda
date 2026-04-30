package com.ascmoda.cart.api.dto;

import com.ascmoda.cart.domain.model.CartStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record CartSummaryResponse(
        UUID id,
        UUID customerId,
        CartStatus status,
        String currency,
        int itemCount,
        int totalQuantity,
        BigDecimal totalAmount,
        int selectedItemCount,
        BigDecimal selectedTotal
) {
}
