package com.ascmoda.inventory.domain.repository;

import com.ascmoda.inventory.domain.model.StockReservation;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface StockReservationRepository extends JpaRepository<StockReservation, UUID>, JpaSpecificationExecutor<StockReservation> {

    @EntityGraph(attributePaths = "inventoryItem")
    Optional<StockReservation> findWithInventoryItemById(UUID id);

    @EntityGraph(attributePaths = "inventoryItem")
    Optional<StockReservation> findByReservationKey(String reservationKey);
}
