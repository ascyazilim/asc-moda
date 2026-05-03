package com.ascmoda.cart.infrastructure.customer;

import java.util.UUID;

public record CustomerIdentityResponse(
        UUID customerId,
        String externalUserId
) {
}
