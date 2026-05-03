package com.ascmoda.customer.controller.dto;

import java.util.UUID;

public record CustomerDefaultAddressesResponse(
        UUID customerId,
        CustomerAddressResponse defaultShippingAddress,
        CustomerAddressResponse defaultBillingAddress
) {
}
