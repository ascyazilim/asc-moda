package com.ascmoda.order.infrastructure.cart;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.UUID;

@FeignClient(name = "cart-service", path = "/api/v1/internal/carts")
public interface CartClient {

    @GetMapping("/{customerId}/checkout-preview")
    CheckoutPreviewResponse getCheckoutPreview(@PathVariable UUID customerId);

    @PostMapping("/{customerId}/mark-checked-out")
    CartResponse markCheckedOut(@PathVariable UUID customerId);
}
