package com.ascmoda.inventory.application.mapper;

import com.ascmoda.inventory.api.dto.StockReservationResponse;
import com.ascmoda.inventory.domain.model.StockReservation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface StockReservationMapper {

    @Mapping(target = "inventoryItemId", source = "inventoryItem.id")
    StockReservationResponse toResponse(StockReservation reservation);
}
