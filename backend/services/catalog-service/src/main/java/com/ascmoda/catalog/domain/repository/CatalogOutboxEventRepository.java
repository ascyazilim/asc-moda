package com.ascmoda.catalog.domain.repository;

import com.ascmoda.catalog.domain.model.CatalogOutboxEvent;
import com.ascmoda.catalog.domain.model.OutboxStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface CatalogOutboxEventRepository extends JpaRepository<CatalogOutboxEvent, UUID> {

    List<CatalogOutboxEvent> findByStatusInAndRetryCountLessThanOrderByOccurredAtAsc(
            Collection<OutboxStatus> statuses,
            int retryCount,
            Pageable pageable
    );

    long countByAggregateIdAndEventType(String aggregateId, String eventType);
}
