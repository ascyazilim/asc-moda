package com.ascmoda.cart.api.dto;

import jakarta.validation.constraints.NotNull;

public record ToggleCartItemsSelectionRequest(
        @NotNull
        Boolean selected
) {
}
