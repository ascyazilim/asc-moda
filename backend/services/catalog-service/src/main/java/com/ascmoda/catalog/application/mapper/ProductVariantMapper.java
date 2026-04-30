package com.ascmoda.catalog.application.mapper;

import com.ascmoda.catalog.api.dto.CreateProductVariantRequest;
import com.ascmoda.catalog.api.dto.ProductVariantResponse;
import com.ascmoda.catalog.domain.model.ProductVariant;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ProductVariantMapper {

    ProductVariantResponse toResponse(ProductVariant variant);

    default ProductVariant toEntity(CreateProductVariantRequest request) {
        return new ProductVariant(
                request.sku(),
                request.color(),
                request.size(),
                request.stockKeepingNote(),
                request.priceOverride(),
                request.active() == null || request.active()
        );
    }
}
