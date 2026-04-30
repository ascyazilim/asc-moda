package com.ascmoda.cart.api.controller;

import com.ascmoda.cart.api.dto.CartResponse;
import com.ascmoda.cart.application.service.CartService;
import com.ascmoda.cart.domain.model.CartStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/carts")
public class AdminCartController {

    private final CartService cartService;

    public AdminCartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public Page<CartResponse> list(
            @RequestParam(required = false) CartStatus status,
            @RequestParam(required = false) UUID customerId,
            @RequestParam(required = false) Instant createdFrom,
            @RequestParam(required = false) Instant createdTo,
            @PageableDefault(size = 20, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return cartService.listAdmin(status, customerId, createdFrom, createdTo, pageable);
    }

    @GetMapping("/{cartId}")
    public CartResponse getById(@PathVariable UUID cartId) {
        return cartService.getById(cartId);
    }
}
