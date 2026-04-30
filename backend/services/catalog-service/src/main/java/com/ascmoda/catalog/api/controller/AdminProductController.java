package com.ascmoda.catalog.api.controller;

import com.ascmoda.catalog.api.dto.CreateProductRequest;
import com.ascmoda.catalog.api.dto.ProductResponse;
import com.ascmoda.catalog.api.dto.UpdateProductRequest;
import com.ascmoda.catalog.application.service.ProductService;
import com.ascmoda.catalog.domain.model.ProductStatus;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/products")
public class AdminProductController {

    private final ProductService productService;

    public AdminProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse create(@Valid @RequestBody CreateProductRequest request) {
        return productService.create(request);
    }

    @PutMapping("/{id}")
    public ProductResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateProductRequest request) {
        return productService.update(id, request);
    }

    @PatchMapping("/{id}/status")
    public ProductResponse changeStatus(@PathVariable UUID id, @RequestParam ProductStatus status) {
        return productService.changeStatus(id, status);
    }

    @GetMapping("/{id}")
    public ProductResponse getById(@PathVariable UUID id) {
        return productService.getAdminById(id);
    }

    @GetMapping
    public Page<ProductResponse> list(@PageableDefault(size = 20) Pageable pageable) {
        return productService.listAdmin(pageable);
    }
}
