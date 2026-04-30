package com.ascmoda.cart.api.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CartValidationResponse(
        UUID cartId,
        UUID customerId,
        boolean valid,
        int itemCount,
        int selectedItemCount,
        BigDecimal totalAmount,
        BigDecimal selectedTotal,
        List<CartValidationIssueResponse> issues
) {
}
