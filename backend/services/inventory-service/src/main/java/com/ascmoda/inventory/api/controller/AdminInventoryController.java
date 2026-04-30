package com.ascmoda.inventory.api.controller;

import com.ascmoda.inventory.api.dto.AdjustStockRequest;
import com.ascmoda.inventory.api.dto.CreateInventoryItemRequest;
import com.ascmoda.inventory.api.dto.InventoryItemResponse;
import com.ascmoda.inventory.api.dto.StockMovementResponse;
import com.ascmoda.inventory.api.dto.UpdateInventoryItemRequest;
import com.ascmoda.inventory.application.service.InventoryService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
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
@RequestMapping("/api/v1/admin/inventory")
public class AdminInventoryController {

    private final InventoryService inventoryService;

    public AdminInventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InventoryItemResponse create(@Valid @RequestBody CreateInventoryItemRequest request) {
        return inventoryService.create(request);
    }

    @PutMapping("/{id}")
    public InventoryItemResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateInventoryItemRequest request) {
        return inventoryService.update(id, request);
    }

    @GetMapping("/{id}")
    public InventoryItemResponse getById(@PathVariable UUID id) {
        return inventoryService.getById(id);
    }

    @GetMapping
    public Page<InventoryItemResponse> list(
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String sku,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return inventoryService.listAdmin(active, sku, pageable);
    }

    @PostMapping("/adjust")
    public InventoryItemResponse adjust(@Valid @RequestBody AdjustStockRequest request) {
        return inventoryService.adjustStock(request);
    }

    @GetMapping("/low-stock")
    public Page<InventoryItemResponse> lowStock(
            @RequestParam(defaultValue = "5") int threshold,
            @PageableDefault(size = 20, sort = "sku", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        return inventoryService.listLowStock(threshold, pageable);
    }

    @GetMapping("/movements")
    public Page<StockMovementResponse> movements(
            @RequestParam(required = false) UUID inventoryItemId,
            @RequestParam(required = false) String sku,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return inventoryService.listMovements(inventoryItemId, sku, pageable);
    }
}
