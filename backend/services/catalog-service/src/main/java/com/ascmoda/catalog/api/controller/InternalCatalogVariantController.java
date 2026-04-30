package com.ascmoda.catalog.api.controller;

import com.ascmoda.catalog.api.dto.ProductVariantDetailResponse;
import com.ascmoda.catalog.api.dto.ProductVariantResponse;
import com.ascmoda.catalog.application.service.ProductVariantService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/internal/catalog/variants")
public class InternalCatalogVariantController {

    private final ProductVariantService productVariantService;

    public InternalCatalogVariantController(ProductVariantService productVariantService) {
        this.productVariantService = productVariantService;
    }

    @GetMapping("/{id}")
    public ProductVariantResponse getById(@PathVariable UUID id) {
        return productVariantService.getById(id);
    }

    @GetMapping("/{id}/details")
    public ProductVariantDetailResponse getDetailsById(@PathVariable UUID id) {
        return productVariantService.getDetailsById(id);
    }
}
