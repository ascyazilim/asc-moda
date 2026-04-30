package com.ascmoda.cart.api.controller;

import com.ascmoda.cart.api.dto.AddCartItemRequest;
import com.ascmoda.cart.api.dto.CartRefreshResponse;
import com.ascmoda.cart.api.dto.CartResponse;
import com.ascmoda.cart.api.dto.CartSummaryResponse;
import com.ascmoda.cart.api.dto.CartValidationResponse;
import com.ascmoda.cart.api.dto.CheckoutPreviewResponse;
import com.ascmoda.cart.api.dto.ToggleCartItemSelectionRequest;
import com.ascmoda.cart.api.dto.ToggleCartItemsSelectionRequest;
import com.ascmoda.cart.api.dto.UpdateCartItemQuantityRequest;
import com.ascmoda.cart.application.service.CartService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/carts")
public class CustomerCartController {

    private final CartService cartService;

    public CustomerCartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping("/{customerId}")
    public CartResponse getOrCreateActiveCart(@PathVariable UUID customerId) {
        return cartService.getOrCreateActiveCart(customerId);
    }

    @GetMapping("/{customerId}/summary")
    public CartSummaryResponse getSummary(@PathVariable UUID customerId) {
        return cartService.getSummary(customerId);
    }

    @PostMapping("/{customerId}/items")
    public CartResponse addItem(@PathVariable UUID customerId, @Valid @RequestBody AddCartItemRequest request) {
        return cartService.addItem(customerId, request);
    }

    @PatchMapping("/{customerId}/items/{itemId}/quantity")
    public CartResponse updateItemQuantity(@PathVariable UUID customerId, @PathVariable UUID itemId,
                                           @Valid @RequestBody UpdateCartItemQuantityRequest request) {
        return cartService.updateItemQuantity(customerId, itemId, request);
    }

    @PatchMapping("/{customerId}/items/{itemId}/selection")
    public CartResponse toggleItemSelection(@PathVariable UUID customerId, @PathVariable UUID itemId,
                                            @Valid @RequestBody ToggleCartItemSelectionRequest request) {
        return cartService.toggleItemSelection(customerId, itemId, request);
    }

    @PatchMapping("/{customerId}/items/selection-all")
    public CartResponse toggleAllItemsSelection(@PathVariable UUID customerId,
                                                @Valid @RequestBody ToggleCartItemsSelectionRequest request) {
        return cartService.toggleAllItemsSelection(customerId, request);
    }

    @DeleteMapping("/{customerId}/items/{itemId}")
    public CartResponse removeItem(@PathVariable UUID customerId, @PathVariable UUID itemId) {
        return cartService.removeItem(customerId, itemId);
    }

    @DeleteMapping("/{customerId}/items")
    public CartResponse clearCart(@PathVariable UUID customerId) {
        return cartService.clearCart(customerId);
    }

    @PostMapping("/{customerId}/refresh")
    public CartRefreshResponse refreshCart(@PathVariable UUID customerId) {
        return cartService.refresh(customerId);
    }

    @GetMapping("/{customerId}/validate")
    public CartValidationResponse validateCart(@PathVariable UUID customerId) {
        return cartService.validateActiveCart(customerId);
    }

    @GetMapping("/{customerId}/checkout-preview")
    public CheckoutPreviewResponse getCheckoutPreview(@PathVariable UUID customerId) {
        return cartService.getCheckoutPreview(customerId);
    }
}
