package com.ascmoda.inventory.api.controller;

import com.ascmoda.inventory.api.dto.AvailabilityResponse;
import com.ascmoda.inventory.api.dto.ConsumeStockReservationRequest;
import com.ascmoda.inventory.api.dto.InventoryItemResponse;
import com.ascmoda.inventory.api.dto.ReleaseStockRequest;
import com.ascmoda.inventory.api.dto.ReserveStockRequest;
import com.ascmoda.inventory.api.dto.StockReservationResponse;
import com.ascmoda.inventory.api.dto.ValidateStockRequest;
import com.ascmoda.inventory.application.service.InventoryService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/internal/inventory")
public class InternalInventoryController {

    private final InventoryService inventoryService;

    public InternalInventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping("/sku/{sku}")
    public InventoryItemResponse getBySku(@PathVariable String sku) {
        return inventoryService.getBySku(sku);
    }

    @GetMapping("/variant/{variantId}")
    public InventoryItemResponse getByProductVariantId(@PathVariable UUID variantId) {
        return inventoryService.getByProductVariantId(variantId);
    }

    @PostMapping("/reserve")
    public StockReservationResponse reserve(@Valid @RequestBody ReserveStockRequest request) {
        return inventoryService.reserve(request);
    }

    @PostMapping("/release")
    public StockReservationResponse release(@Valid @RequestBody ReleaseStockRequest request) {
        return inventoryService.release(request);
    }

    @PostMapping("/consume")
    public StockReservationResponse consume(@Valid @RequestBody ConsumeStockReservationRequest request) {
        return inventoryService.consume(request);
    }

    @GetMapping("/reservations/{reservationId}")
    public StockReservationResponse getReservation(@PathVariable UUID reservationId) {
        return inventoryService.getReservation(reservationId);
    }

    @GetMapping("/variant/{variantId}/availability")
    public AvailabilityResponse getAvailability(@PathVariable UUID variantId) {
        return inventoryService.getAvailabilityByProductVariantId(variantId);
    }

    @PostMapping("/validate")
    public AvailabilityResponse validateStock(@Valid @RequestBody ValidateStockRequest request) {
        return inventoryService.validateStock(request);
    }
}
