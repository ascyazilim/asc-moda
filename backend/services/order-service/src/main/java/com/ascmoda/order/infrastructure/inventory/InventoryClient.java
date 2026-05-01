package com.ascmoda.order.infrastructure.inventory;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "inventory-service", path = "/api/v1/internal/inventory")
public interface InventoryClient {

    @PostMapping("/reserve")
    StockReservationResponse reserve(@RequestBody ReserveStockRequest request);

    @PostMapping("/release")
    StockReservationResponse release(@RequestBody ReleaseStockRequest request);

    @PostMapping("/consume")
    StockReservationResponse consume(@RequestBody ConsumeStockReservationRequest request);
}
