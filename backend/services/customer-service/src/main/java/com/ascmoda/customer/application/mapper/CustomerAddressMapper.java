package com.ascmoda.customer.application.mapper;

import com.ascmoda.customer.controller.dto.CustomerAddressResponse;
import com.ascmoda.customer.domain.model.CustomerAddress;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CustomerAddressMapper {

    @Mapping(target = "customerId", source = "customer.id")
    CustomerAddressResponse toResponse(CustomerAddress address);
}
