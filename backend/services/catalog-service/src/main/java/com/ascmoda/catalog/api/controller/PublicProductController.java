package com.ascmoda.catalog.api.controller;

import com.ascmoda.catalog.api.dto.ProductResponse;
import com.ascmoda.catalog.application.service.ProductService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/catalog/products")
public class PublicProductController {

    private final ProductService productService;

    public PublicProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public Page<ProductResponse> list(
            @RequestParam(required = false) String categorySlug,
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return productService.listPublic(categorySlug, q, pageable);
    }

    @GetMapping("/{slug}")
    public ProductResponse getBySlug(@PathVariable String slug) {
        return productService.getPublicBySlug(slug);
    }
}
