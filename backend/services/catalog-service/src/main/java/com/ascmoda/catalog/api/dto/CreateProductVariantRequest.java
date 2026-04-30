package com.ascmoda.catalog.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateProductVariantRequest(
        @NotBlank
        @Size(max = 120)
        String sku,

        @Size(max = 80)
        String color,

        @Size(max = 40)
        String size,

        @Size(max = 500)
        String stockKeepingNote,

        @DecimalMin("0.00")
        BigDecimal priceOverride,

        Boolean active
) {
}
