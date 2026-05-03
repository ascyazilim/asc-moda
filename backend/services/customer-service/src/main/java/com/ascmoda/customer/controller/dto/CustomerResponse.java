package com.ascmoda.customer.controller.dto;

import com.ascmoda.customer.domain.model.CustomerStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CustomerResponse(
        UUID id,
        String externalUserId,
        String email,
        String phoneNumber,
        String firstName,
        String lastName,
        String fullName,
        CustomerStatus status,
        boolean emailVerified,
        boolean phoneVerified,
        boolean marketingConsent,
        List<CustomerAddressResponse> addresses,
        Long version,
        Instant createdAt,
        Instant updatedAt
) {
}
