package com.ascmoda.customer.controller.dto;

import com.ascmoda.customer.domain.model.AddressType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateCustomerAddressRequest(
        @NotBlank @Size(max = 80) String title,
        @NotNull AddressType addressType,
        @NotBlank @Size(max = 240) String fullName,
        @NotBlank @Size(max = 40) String phoneNumber,
        @NotBlank @Size(max = 120) String city,
        @NotBlank @Size(max = 120) String district,
        @NotBlank @Size(max = 1000) String addressLine,
        @Size(max = 20) String postalCode,
        @NotBlank @Size(max = 80) String country,
        boolean defaultShipping,
        boolean defaultBilling
) {
}
