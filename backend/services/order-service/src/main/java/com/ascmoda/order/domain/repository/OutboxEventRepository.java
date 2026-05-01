package com.ascmoda.order.domain.repository;

import com.ascmoda.order.domain.model.OutboxEvent;
import com.ascmoda.order.domain.model.OutboxStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    List<OutboxEvent> findByStatusInAndRetryCountLessThanOrderByOccurredAtAsc(
            Collection<OutboxStatus> statuses,
            int retryCount,
            Pageable pageable
    );

    long countByAggregateIdAndEventType(String aggregateId, String eventType);
}
