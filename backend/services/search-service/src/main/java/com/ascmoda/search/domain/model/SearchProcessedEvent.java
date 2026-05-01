package com.ascmoda.search.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "search_processed_events",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_search_processed_events_event_id", columnNames = "event_id")
        },
        indexes = {
                @Index(name = "idx_search_processed_events_status", columnList = "status"),
                @Index(name = "idx_search_processed_events_event_type", columnList = "event_type"),
                @Index(name = "idx_search_processed_events_processed_at", columnList = "processed_at"),
                @Index(name = "idx_search_processed_events_created_at", columnList = "created_at"),
                @Index(name = "idx_search_processed_events_reference", columnList = "reference_type,reference_id")
        }
)
public class SearchProcessedEvent {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "event_id", nullable = false, updatable = false)
    private UUID eventId;

    @Column(name = "event_type", nullable = false, length = 120)
    private String eventType;

    @Column(name = "correlation_id", nullable = false, length = 160)
    private String correlationId;

    @Column(name = "reference_type", nullable = false, length = 80)
    private String referenceType;

    @Column(name = "reference_id", nullable = false, length = 120)
    private String referenceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ProcessedEventStatus status = ProcessedEventStatus.PROCESSING;

    @Column(name = "failure_reason", length = 1000)
    private String failureReason;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected SearchProcessedEvent() {
    }

    public SearchProcessedEvent(UUID eventId, String eventType, String correlationId,
                                String referenceType, String referenceId) {
        this.id = UUID.randomUUID();
        this.eventId = requireUuid(eventId, "Event id must be provided");
        this.eventType = requireText(eventType, "Event type must be provided");
        this.correlationId = requireText(correlationId, "Correlation id must be provided");
        this.referenceType = requireText(referenceType, "Reference type must be provided");
        this.referenceId = requireText(referenceId, "Reference id must be provided");
        this.status = ProcessedEventStatus.PROCESSING;
    }

    public void markProcessing() {
        this.status = ProcessedEventStatus.PROCESSING;
        this.failureReason = null;
    }

    public void markProcessed(Instant processedAt) {
        this.status = ProcessedEventStatus.PROCESSED;
        this.processedAt = processedAt == null ? Instant.now() : processedAt;
        this.failureReason = null;
    }

    public void markFailed(String failureReason) {
        this.status = ProcessedEventStatus.FAILED;
        this.processedAt = Instant.now();
        this.failureReason = truncate(failureReason, 1000);
    }

    public boolean isProcessed() {
        return status == ProcessedEventStatus.PROCESSED;
    }

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    private UUID requireUuid(UUID value, String message) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    public UUID getId() {
        return id;
    }

    public UUID getEventId() {
        return eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public String getReferenceType() {
        return referenceType;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public ProcessedEventStatus getStatus() {
        return status;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }
}
