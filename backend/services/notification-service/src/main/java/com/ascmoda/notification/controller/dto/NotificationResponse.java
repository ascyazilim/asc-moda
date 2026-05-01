package com.ascmoda.notification.controller.dto;

import com.ascmoda.notification.domain.model.NotificationChannel;
import com.ascmoda.notification.domain.model.NotificationStatus;
import com.ascmoda.notification.domain.model.NotificationType;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        UUID eventId,
        String correlationId,
        NotificationType notificationType,
        NotificationChannel channel,
        String recipient,
        String subject,
        String body,
        NotificationStatus status,
        String referenceType,
        String referenceId,
        String sourceService,
        String failureReason,
        int retryCount,
        Instant processedAt,
        Instant sentAt,
        Long version,
        Instant createdAt,
        Instant updatedAt
) {
}
