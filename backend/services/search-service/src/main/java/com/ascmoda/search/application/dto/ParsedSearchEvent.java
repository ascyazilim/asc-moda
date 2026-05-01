package com.ascmoda.search.application.dto;

import java.time.Instant;
import java.util.UUID;

public record ParsedSearchEvent(
        UUID eventId,
        String eventType,
        Instant occurredAt,
        String sourceService,
        String correlationId,
        Object payload,
        String payloadJson
) {
}
