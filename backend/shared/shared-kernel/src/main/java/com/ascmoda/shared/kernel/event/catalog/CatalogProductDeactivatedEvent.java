package com.ascmoda.shared.kernel.event.catalog;

import java.time.Instant;
import java.util.UUID;

public record CatalogProductDeactivatedEvent(
        UUID productId,
        String productSlug,
        String status,
        Instant occurredAt,
        String sourceService
) {
}
