package com.ascmoda.catalog.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCategoryRequest(
        @NotBlank
        @Size(max = 160)
        String name,

        @Size(max = 180)
        String slug,

        @Size(max = 1000)
        String description,

        Boolean active
) {
}
