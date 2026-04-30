package com.ascmoda.inventory.application.mapper;

import com.ascmoda.inventory.api.dto.InventoryItemResponse;
import com.ascmoda.inventory.domain.model.InventoryItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface InventoryItemMapper {

    @Mapping(target = "availableQuantity", expression = "java(item.availableQuantity())")
    InventoryItemResponse toResponse(InventoryItem item);
}
