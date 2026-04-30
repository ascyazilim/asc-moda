package com.ascmoda.inventory.domain.repository;

import com.ascmoda.inventory.domain.model.StockMovement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface StockMovementRepository extends JpaRepository<StockMovement, UUID>, JpaSpecificationExecutor<StockMovement> {

    Page<StockMovement> findByInventoryItemIdOrderByCreatedAtDesc(UUID inventoryItemId, Pageable pageable);

    Page<StockMovement> findBySkuOrderByCreatedAtDesc(String sku, Pageable pageable);

    Page<StockMovement> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
