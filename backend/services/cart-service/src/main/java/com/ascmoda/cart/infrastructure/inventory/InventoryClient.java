package com.ascmoda.cart.infrastructure.inventory;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "inventory-service", path = "/api/v1/internal/inventory")
public interface InventoryClient {

    @GetMapping("/variant/{variantId}")
    InventoryItemResponse getByProductVariantId(@PathVariable UUID variantId);
}
