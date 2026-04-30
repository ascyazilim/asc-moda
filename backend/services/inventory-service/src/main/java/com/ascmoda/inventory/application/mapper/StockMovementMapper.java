package com.ascmoda.inventory.application.mapper;

import com.ascmoda.inventory.api.dto.StockMovementResponse;
import com.ascmoda.inventory.domain.model.StockMovement;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface StockMovementMapper {

    @Mapping(target = "inventoryItemId", source = "inventoryItem.id")
    StockMovementResponse toResponse(StockMovement movement);
}
