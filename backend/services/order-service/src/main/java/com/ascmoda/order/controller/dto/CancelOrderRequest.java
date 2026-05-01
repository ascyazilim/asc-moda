package com.ascmoda.order.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CancelOrderRequest(
        @NotBlank
        @Size(max = 500)
        String reason
) {
}
