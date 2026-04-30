package com.ascmoda.catalog.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record ProductImageRequest(
        UUID id,
        UUID variantId,

        @NotBlank
        @Size(max = 1000)
        String imageUrl,

        @Size(max = 300)
        String altText,

        @Min(0)
        Integer sortOrder,

        Boolean main,
        Boolean active
) {
}
