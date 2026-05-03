package com.ascmoda.customer.controller.dto;

import com.ascmoda.customer.domain.model.CustomerStatus;

import java.util.UUID;

public record CustomerSummaryResponse(
        UUID customerId,
        String externalUserId,
        String fullName,
        String displayName,
        String email,
        String phoneNumber,
        CustomerStatus status,
        boolean emailVerified,
        boolean phoneVerified,
        boolean hasDefaultShippingAddress,
        boolean hasDefaultBillingAddress,
        CustomerAddressResponse defaultShippingAddress,
        CustomerAddressResponse defaultBillingAddress
) {
}
