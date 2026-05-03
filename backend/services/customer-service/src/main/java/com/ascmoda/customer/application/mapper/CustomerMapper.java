package com.ascmoda.customer.application.mapper;

import com.ascmoda.customer.controller.dto.CustomerResponse;
import com.ascmoda.customer.controller.dto.CustomerSummaryResponse;
import com.ascmoda.customer.domain.model.Customer;
import com.ascmoda.customer.domain.model.CustomerAddress;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = CustomerAddressMapper.class)
public interface CustomerMapper {

    @Mapping(target = "fullName", expression = "java(customer.fullName())")
    CustomerResponse toResponse(Customer customer);

    @Mapping(target = "customerId", source = "customer.id")
    @Mapping(target = "fullName", expression = "java(customer.fullName())")
    @Mapping(target = "email", source = "customer.email")
    @Mapping(target = "phoneNumber", source = "customer.phoneNumber")
    @Mapping(target = "status", source = "customer.status")
    @Mapping(target = "defaultShippingAddress", source = "defaultShippingAddress")
    @Mapping(target = "defaultBillingAddress", source = "defaultBillingAddress")
    CustomerSummaryResponse toSummaryResponse(
            Customer customer,
            CustomerAddress defaultShippingAddress,
            CustomerAddress defaultBillingAddress
    );
}
