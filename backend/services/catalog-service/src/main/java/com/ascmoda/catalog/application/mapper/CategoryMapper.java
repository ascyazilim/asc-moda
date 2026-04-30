package com.ascmoda.catalog.application.mapper;

import com.ascmoda.catalog.api.dto.CategoryResponse;
import com.ascmoda.catalog.domain.model.Category;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

    CategoryResponse toResponse(Category category);
}
