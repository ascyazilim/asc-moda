package com.ascmoda.customer.controller.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCustomerRequest(
        @Size(max = 120) String externalUserId,
        @NotBlank @Email @Size(max = 320) String email,
        @Size(max = 40) String phoneNumber,
        @NotBlank @Size(max = 120) String firstName,
        @NotBlank @Size(max = 120) String lastName,
        boolean emailVerified,
        boolean phoneVerified,
        boolean marketingConsent
) {
}
