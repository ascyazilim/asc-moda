package com.ascmoda.customer.controller;

import com.ascmoda.customer.application.service.CustomerService;
import com.ascmoda.customer.controller.dto.ChangeCustomerStatusRequest;
import com.ascmoda.customer.controller.dto.CustomerResponse;
import com.ascmoda.customer.controller.dto.CustomerSummaryResponse;
import com.ascmoda.customer.domain.model.CustomerStatus;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/customers")
public class AdminCustomerController {

    private final CustomerService customerService;

    public AdminCustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping
    public Page<CustomerSummaryResponse> list(
            @RequestParam(required = false) CustomerStatus status,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String phoneNumber,
            @RequestParam(required = false) Instant createdFrom,
            @RequestParam(required = false) Instant createdTo,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return customerService.listAdmin(status, email, phoneNumber, createdFrom, createdTo, pageable);
    }

    @GetMapping("/{customerId}")
    public CustomerResponse get(@PathVariable UUID customerId) {
        return customerService.getCustomer(customerId);
    }

    @PatchMapping("/{customerId}/status")
    public CustomerResponse changeStatus(@PathVariable UUID customerId,
                                         @Valid @RequestBody ChangeCustomerStatusRequest request) {
        return customerService.changeStatus(customerId, request);
    }
}
