package com.ascmoda.customer.controller.dto;

import com.ascmoda.customer.domain.model.CustomerStatus;

import java.util.UUID;

public record CustomerSummaryResponse(
        UUID customerId,
        String fullName,
        String email,
        String phoneNumber,
        CustomerStatus status,
        CustomerAddressResponse defaultShippingAddress,
        CustomerAddressResponse defaultBillingAddress
) {
}
