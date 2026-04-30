package com.ascmoda.cart.application.mapper;

import com.ascmoda.cart.api.dto.CartItemResponse;
import com.ascmoda.cart.domain.model.CartItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CartItemMapper {

    @Mapping(target = "lineTotal", expression = "java(item.lineTotal())")
    CartItemResponse toResponse(CartItem item);
}
