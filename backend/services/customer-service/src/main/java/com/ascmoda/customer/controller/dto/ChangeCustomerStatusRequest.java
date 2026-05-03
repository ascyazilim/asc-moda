package com.ascmoda.customer.controller.dto;

import com.ascmoda.customer.domain.model.CustomerStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeCustomerStatusRequest(
        @NotNull CustomerStatus status
) {
}
