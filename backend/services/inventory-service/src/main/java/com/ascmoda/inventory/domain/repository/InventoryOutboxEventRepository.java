package com.ascmoda.inventory.domain.repository;

import com.ascmoda.inventory.domain.model.InventoryOutboxEvent;
import com.ascmoda.inventory.domain.model.OutboxStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface InventoryOutboxEventRepository extends JpaRepository<InventoryOutboxEvent, UUID> {

    List<InventoryOutboxEvent> findByStatusInAndRetryCountLessThanOrderByOccurredAtAsc(
            Collection<OutboxStatus> statuses,
            int retryCount,
            Pageable pageable
    );

    long countByAggregateIdAndEventType(String aggregateId, String eventType);
}
