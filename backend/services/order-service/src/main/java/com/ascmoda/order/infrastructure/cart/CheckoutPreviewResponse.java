package com.ascmoda.order.infrastructure.cart;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CheckoutPreviewResponse(
        UUID cartId,
        UUID customerId,
        String currency,
        List<CartItemResponse> selectedItems,
        int selectedItemCount,
        BigDecimal selectedTotal,
        CartValidationResponse validation
) {
}
