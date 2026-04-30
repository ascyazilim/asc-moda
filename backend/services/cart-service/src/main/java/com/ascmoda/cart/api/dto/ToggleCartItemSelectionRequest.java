package com.ascmoda.cart.api.dto;

import jakarta.validation.constraints.NotNull;

public record ToggleCartItemSelectionRequest(
        @NotNull
        Boolean selected
) {
}
