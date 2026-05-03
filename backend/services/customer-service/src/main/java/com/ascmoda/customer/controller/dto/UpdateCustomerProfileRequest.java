package com.ascmoda.customer.controller.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UpdateCustomerProfileRequest(
        @Size(max = 120) String externalUserId,
        @Email @Size(max = 320) String email,
        @Size(max = 40) String phoneNumber,
        @Size(max = 120) String firstName,
        @Size(max = 120) String lastName,
        Boolean emailVerified,
        Boolean phoneVerified,
        Boolean marketingConsent
) {
}
