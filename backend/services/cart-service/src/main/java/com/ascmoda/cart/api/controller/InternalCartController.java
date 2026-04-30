package com.ascmoda.cart.api.controller;

import com.ascmoda.cart.api.dto.CheckoutPreviewResponse;
import com.ascmoda.cart.api.dto.CartResponse;
import com.ascmoda.cart.application.service.CartService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/internal/carts")
public class InternalCartController {

    private final CartService cartService;

    public InternalCartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping("/{customerId}")
    public CartResponse getActiveCart(@PathVariable UUID customerId) {
        return cartService.getActiveCart(customerId);
    }

    @GetMapping("/{customerId}/checkout-preview")
    public CheckoutPreviewResponse getCheckoutPreview(@PathVariable UUID customerId) {
        return cartService.getCheckoutPreview(customerId);
    }

    @PostMapping("/{customerId}/mark-checked-out")
    public CartResponse markCheckedOut(@PathVariable UUID customerId) {
        return cartService.markActiveCartCheckedOut(customerId);
    }

    @PostMapping("/{customerId}/mark-abandoned")
    public CartResponse markAbandoned(@PathVariable UUID customerId) {
        return cartService.markActiveCartAbandoned(customerId);
    }
}
