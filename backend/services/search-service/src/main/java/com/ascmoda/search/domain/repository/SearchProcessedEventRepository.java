package com.ascmoda.search.domain.repository;

import com.ascmoda.search.domain.model.SearchProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SearchProcessedEventRepository extends JpaRepository<SearchProcessedEvent, UUID> {

    Optional<SearchProcessedEvent> findByEventId(UUID eventId);
}
