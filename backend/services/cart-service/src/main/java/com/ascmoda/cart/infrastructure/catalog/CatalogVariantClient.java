package com.ascmoda.cart.infrastructure.catalog;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "catalog-service", path = "/api/v1/internal/catalog")
public interface CatalogVariantClient {

    @GetMapping("/variants/{id}/details")
    CatalogVariantDetailResponse getVariantDetails(@PathVariable UUID id);
}
