package com.ascmoda.order.controller.dto;

public record AddressSnapshotResponse(
        String fullName,
        String phoneNumber,
        String city,
        String district,
        String addressLine,
        String postalCode,
        String country
) {
}
