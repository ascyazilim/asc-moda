package com.ascmoda.order.controller.dto;

import com.ascmoda.order.domain.model.OrderSource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateOrderRequest(
        @NotNull
        UUID customerId,

        @NotNull
        @Valid
        AddressSnapshotRequest shippingAddress,

        @Size(max = 1000)
        String note,

        OrderSource source
) {
}
