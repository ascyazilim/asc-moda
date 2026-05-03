package com.ascmoda.customer.controller.dto;

import com.ascmoda.customer.domain.model.CustomerStatus;

import java.util.UUID;

public record CustomerDefaultAddressesResponse(
        UUID customerId,
        String externalUserId,
        String fullName,
        String displayName,
        CustomerStatus status,
        boolean hasActiveAddress,
        boolean hasDefaultShippingAddress,
        boolean hasDefaultBillingAddress,
        CustomerAddressResponse defaultShippingAddress,
        CustomerAddressResponse defaultBillingAddress
) {
}
