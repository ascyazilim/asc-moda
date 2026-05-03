package com.ascmoda.customer.controller.dto;

import com.ascmoda.customer.domain.model.AddressType;

import java.time.Instant;
import java.util.UUID;

public record CustomerAddressResponse(
        UUID id,
        UUID customerId,
        String title,
        AddressType addressType,
        String fullName,
        String phoneNumber,
        String city,
        String district,
        String addressLine,
        String postalCode,
        String country,
        boolean defaultShipping,
        boolean defaultBilling,
        boolean active,
        Long version,
        Instant createdAt,
        Instant updatedAt
) {
}
