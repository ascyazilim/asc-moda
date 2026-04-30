package com.ascmoda.inventory.api.dto;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record ConsumeStockReservationRequest(
        UUID reservationId,

        @Size(max = 160)
        String reservationKey,

        @Positive
        Integer quantity,

        @Size(max = 1000)
        String note
) {
}
