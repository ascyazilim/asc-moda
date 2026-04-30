package com.ascmoda.catalog.application.mapper;

import com.ascmoda.catalog.api.dto.ProductImageResponse;
import com.ascmoda.catalog.domain.model.ProductImage;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProductImageMapper {

    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "variantId", source = "variant.id")
    ProductImageResponse toResponse(ProductImage image);
}
