package com.ascmoda.order.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddressSnapshotRequest(
        @NotBlank
        @Size(max = 160)
        String fullName,

        @NotBlank
        @Size(max = 40)
        String phoneNumber,

        @NotBlank
        @Size(max = 80)
        String city,

        @NotBlank
        @Size(max = 120)
        String district,

        @NotBlank
        @Size(max = 500)
        String addressLine,

        @Size(max = 20)
        String postalCode,

        @Size(max = 80)
        String country
) {
}
