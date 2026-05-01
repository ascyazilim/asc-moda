package com.ascmoda.notification.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "notification_messages",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_notification_messages_event_id", columnNames = "event_id")
        },
        indexes = {
                @Index(name = "idx_notification_messages_status", columnList = "status"),
                @Index(name = "idx_notification_messages_type", columnList = "notification_type"),
                @Index(name = "idx_notification_messages_channel", columnList = "channel"),
                @Index(name = "idx_notification_messages_reference", columnList = "reference_type,reference_id"),
                @Index(name = "idx_notification_messages_created_at", columnList = "created_at")
        }
)
public class NotificationMessage extends BaseAuditableEntity {

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "correlation_id", nullable = false, length = 160)
    private String correlationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 50)
    private NotificationType notificationType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationChannel channel;

    @Column(nullable = false, length = 240)
    private String recipient;

    @Column(nullable = false, length = 240)
    private String subject;

    @Column(nullable = false, length = 2000)
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationStatus status = NotificationStatus.RECEIVED;

    @Column(name = "reference_type", nullable = false, length = 80)
    private String referenceType;

    @Column(name = "reference_id", nullable = false, length = 120)
    private String referenceId;

    @Column(name = "source_service", nullable = false, length = 120)
    private String sourceService;

    @Column(name = "payload_json", nullable = false, columnDefinition = "TEXT")
    private String payloadJson;

    @Column(name = "failure_reason", length = 1000)
    private String failureReason;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    protected NotificationMessage() {
    }

    public NotificationMessage(UUID eventId, String correlationId, NotificationType notificationType,
                               NotificationChannel channel, String recipient, String subject, String body,
                               String referenceType, String referenceId, String sourceService, String payloadJson) {
        this.eventId = requireUuid(eventId, "Event id must be provided");
        this.correlationId = requireText(correlationId, "Correlation id must be provided");
        this.notificationType = notificationType;
        this.channel = channel;
        this.recipient = requireText(recipient, "Recipient must be provided");
        this.subject = requireText(subject, "Subject must be provided");
        this.body = requireText(body, "Body must be provided");
        this.referenceType = requireText(referenceType, "Reference type must be provided");
        this.referenceId = requireText(referenceId, "Reference id must be provided");
        this.sourceService = requireText(sourceService, "Source service must be provided");
        this.payloadJson = requireText(payloadJson, "Payload JSON must be provided");
        this.status = NotificationStatus.RECEIVED;
    }

    public void markPending() {
        this.status = NotificationStatus.PENDING;
        this.failureReason = null;
    }

    public void markSent(Instant sentAt) {
        this.status = NotificationStatus.SENT;
        this.processedAt = sentAt == null ? Instant.now() : sentAt;
        this.sentAt = this.processedAt;
        this.failureReason = null;
    }

    public void markFailed(String failureReason) {
        this.status = NotificationStatus.FAILED;
        this.processedAt = Instant.now();
        this.retryCount++;
        this.failureReason = truncate(failureReason, 1000);
    }

    public void markSkipped(String reason) {
        this.status = NotificationStatus.SKIPPED;
        this.processedAt = Instant.now();
        this.failureReason = truncate(reason, 1000);
    }

    public boolean isRetryable() {
        return status == NotificationStatus.FAILED;
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

    public UUID getEventId() {
        return eventId;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public NotificationType getNotificationType() {
        return notificationType;
    }

    public NotificationChannel getChannel() {
        return channel;
    }

    public String getRecipient() {
        return recipient;
    }

    public String getSubject() {
        return subject;
    }

    public String getBody() {
        return body;
    }

    public NotificationStatus getStatus() {
        return status;
    }

    public String getReferenceType() {
        return referenceType;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public String getSourceService() {
        return sourceService;
    }

    public String getPayloadJson() {
        return payloadJson;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }

    public Instant getSentAt() {
        return sentAt;
    }
}
