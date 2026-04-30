package com.ascmoda.catalog.api.dto;

import com.ascmoda.catalog.domain.model.ProductStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record UpdateProductRequest(
        @NotBlank
        @Size(max = 220)
        String name,

        @Size(max = 240)
        String slug,

        @Size(max = 5000)
        String description,

        @Size(max = 500)
        String shortDescription,

        @NotNull
        @DecimalMin("0.00")
        BigDecimal basePrice,

        ProductStatus status,

        @NotNull
        UUID categoryId,

        @Valid
        List<CreateProductVariantRequest> variants,

        @Valid
        List<ProductImageRequest> images
) {
}
