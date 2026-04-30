package com.ascmoda.inventory.domain.repository;

import com.ascmoda.inventory.domain.model.InventoryItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, UUID> {

    Optional<InventoryItem> findBySku(String sku);

    Optional<InventoryItem> findByProductVariantId(UUID productVariantId);

    Page<InventoryItem> findByActiveTrue(Pageable pageable);

    @Query("""
            select i
            from InventoryItem i
            where (:active is null or i.active = :active)
              and (:sku = '' or lower(i.sku) like concat('%', :sku, '%'))
            """)
    Page<InventoryItem> searchAdmin(@Param("active") Boolean active, @Param("sku") String sku, Pageable pageable);

    @Query("""
            select i
            from InventoryItem i
            where i.active = true
              and (i.quantityOnHand - i.reservedQuantity) <= :threshold
            """)
    Page<InventoryItem> findLowStock(@Param("threshold") int threshold, Pageable pageable);

    boolean existsBySku(String sku);

    boolean existsByProductVariantId(UUID productVariantId);

    boolean existsBySkuAndIdNot(String sku, UUID id);

    boolean existsByProductVariantIdAndIdNot(UUID productVariantId, UUID id);
}
