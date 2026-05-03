package com.ascmoda.customer.controller.dto;

import com.ascmoda.customer.domain.model.AddressType;
import jakarta.validation.constraints.Size;

public record UpdateCustomerAddressRequest(
        @Size(max = 80) String title,
        AddressType addressType,
        @Size(max = 240) String fullName,
        @Size(max = 40) String phoneNumber,
        @Size(max = 120) String city,
        @Size(max = 120) String district,
        @Size(max = 1000) String addressLine,
        @Size(max = 20) String postalCode,
        @Size(max = 80) String country,
        Boolean defaultShipping,
        Boolean defaultBilling
) {
}
