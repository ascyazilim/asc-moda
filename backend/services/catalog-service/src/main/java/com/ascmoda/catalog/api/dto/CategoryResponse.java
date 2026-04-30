package com.ascmoda.catalog.api.dto;

import java.time.Instant;
import java.util.UUID;

public record CategoryResponse(
        UUID id,
        String name,
        String slug,
        String description,
        boolean active,
        Long version,
        Instant createdAt,
        Instant updatedAt
) {
}
