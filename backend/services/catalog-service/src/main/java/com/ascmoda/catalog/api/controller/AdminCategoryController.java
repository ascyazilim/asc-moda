package com.ascmoda.catalog.api.controller;

import com.ascmoda.catalog.api.dto.CategoryResponse;
import com.ascmoda.catalog.api.dto.CreateCategoryRequest;
import com.ascmoda.catalog.api.dto.UpdateCategoryRequest;
import com.ascmoda.catalog.application.service.CategoryService;
import jakarta.validation.Valid;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/categories")
public class AdminCategoryController {

    private final CategoryService categoryService;

    public AdminCategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse create(@Valid @RequestBody CreateCategoryRequest request) {
        return categoryService.create(request);
    }

    @PutMapping("/{id}")
    public CategoryResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateCategoryRequest request) {
        return categoryService.update(id, request);
    }

    @PatchMapping("/{id}/activate")
    public CategoryResponse activate(@PathVariable UUID id) {
        return categoryService.activate(id);
    }

    @PatchMapping("/{id}/deactivate")
    public CategoryResponse deactivate(@PathVariable UUID id) {
        return categoryService.deactivate(id);
    }

    @GetMapping
    public List<CategoryResponse> list(@RequestParam(required = false) Boolean active) {
        return categoryService.listAllForAdmin(active);
    }
}
