package com.ascmoda.shared.kernel.event;

import java.time.Instant;
import java.util.UUID;

public record EventEnvelope<T>(
        UUID eventId,
        String eventType,
        Instant occurredAt,
        String sourceService,
        String correlationId,
        T payload
) {
}
