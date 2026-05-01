package com.ascmoda.inventory.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "inventory_outbox_events",
        indexes = {
                @Index(name = "idx_inventory_outbox_status_occurred_at", columnList = "status,occurred_at"),
                @Index(name = "idx_inventory_outbox_published_at", columnList = "published_at"),
                @Index(name = "idx_inventory_outbox_aggregate", columnList = "aggregate_type,aggregate_id"),
                @Index(name = "idx_inventory_outbox_event_type", columnList = "event_type"),
                @Index(name = "idx_inventory_outbox_correlation_id", columnList = "correlation_id")
        }
)
public class InventoryOutboxEvent {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "aggregate_type", nullable = false, length = 80)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false, length = 120)
    private String aggregateId;

    @Column(name = "event_type", nullable = false, length = 120)
    private String eventType;

    @Column(name = "payload_json", nullable = false, columnDefinition = "TEXT")
    private String payloadJson;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OutboxStatus status = OutboxStatus.PENDING;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "last_error", length = 1000)
    private String lastError;

    @Column(name = "correlation_id", nullable = false, length = 160)
    private String correlationId;

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected InventoryOutboxEvent() {
    }

    public InventoryOutboxEvent(UUID id, String aggregateType, String aggregateId, String eventType, String payloadJson,
                                Instant occurredAt, String correlationId) {
        this.id = id == null ? UUID.randomUUID() : id;
        this.aggregateType = requireText(aggregateType, "Aggregate type must be provided");
        this.aggregateId = requireText(aggregateId, "Aggregate id must be provided");
        this.eventType = requireText(eventType, "Event type must be provided");
        this.payloadJson = requireText(payloadJson, "Payload JSON must be provided");
        this.occurredAt = occurredAt == null ? Instant.now() : occurredAt;
        this.correlationId = requireText(correlationId, "Correlation id must be provided");
        this.status = OutboxStatus.PENDING;
    }

    public void markPublished(Instant publishedAt) {
        this.status = OutboxStatus.PUBLISHED;
        this.publishedAt = publishedAt == null ? Instant.now() : publishedAt;
        this.lastError = null;
    }

    public void markFailed(String error) {
        this.status = OutboxStatus.FAILED;
        this.retryCount++;
        this.lastError = truncate(error, 1000);
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

    public String getAggregateType() {
        return aggregateType;
    }

    public String getAggregateId() {
        return aggregateId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getPayloadJson() {
        return payloadJson;
    }

    public OutboxStatus getStatus() {
        return status;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public String getLastError() {
        return lastError;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public Long getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
