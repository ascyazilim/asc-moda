package com.ascmoda.notification.application.dto;

import java.time.Instant;
import java.util.UUID;

public record ParsedNotificationEvent(
        UUID eventId,
        String eventType,
        Instant occurredAt,
        String sourceService,
        String correlationId,
        Object payload,
        String payloadJson
) {
}
