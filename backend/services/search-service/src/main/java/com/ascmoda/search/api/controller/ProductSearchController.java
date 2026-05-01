package com.ascmoda.search.api.controller;

import com.ascmoda.search.application.dto.ProductSearchResponse;
import com.ascmoda.search.application.dto.SearchPageResponse;
import com.ascmoda.search.application.service.ProductSearchIndexService;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@Validated
@RestController
@RequestMapping("/api/v1/search/products")
public class ProductSearchController {

    private final ProductSearchIndexService productSearchIndexService;

    public ProductSearchController(ProductSearchIndexService productSearchIndexService) {
        this.productSearchIndexService = productSearchIndexService;
    }

    @GetMapping
    public SearchPageResponse<ProductSearchResponse> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String categorySlug,
            @RequestParam(required = false) @DecimalMin("0.00") BigDecimal minPrice,
            @RequestParam(required = false) @DecimalMin("0.00") BigDecimal maxPrice,
            @RequestParam(defaultValue = "relevance") String sort,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return productSearchIndexService.search(q, categorySlug, minPrice, maxPrice, sort, page, size);
    }

    @GetMapping("/{slug}")
    public ProductSearchResponse getBySlug(@PathVariable String slug) {
        return productSearchIndexService.getBySlug(slug);
    }
}
