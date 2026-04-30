package com.ascmoda.catalog.application.mapper;

import com.ascmoda.catalog.api.dto.ProductResponse;
import com.ascmoda.catalog.domain.model.Product;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(
        componentModel = "spring",
        uses = {
                ProductVariantMapper.class,
                ProductImageMapper.class
        }
)
public interface ProductMapper {

    @Mapping(target = "categoryId", source = "category.id")
    @Mapping(target = "categoryName", source = "category.name")
    ProductResponse toResponse(Product product);
}
