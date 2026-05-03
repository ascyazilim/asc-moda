package com.ascmoda.customer.controller;

import com.ascmoda.customer.application.service.CustomerAddressService;
import com.ascmoda.customer.application.service.CustomerService;
import com.ascmoda.customer.controller.dto.CustomerDefaultAddressesResponse;
import com.ascmoda.customer.controller.dto.CustomerResponse;
import com.ascmoda.customer.controller.dto.CustomerSummaryResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/internal/customers")
public class InternalCustomerController {

    private final CustomerService customerService;
    private final CustomerAddressService customerAddressService;

    public InternalCustomerController(CustomerService customerService, CustomerAddressService customerAddressService) {
        this.customerService = customerService;
        this.customerAddressService = customerAddressService;
    }

    @GetMapping("/{customerId}")
    public CustomerResponse get(@PathVariable UUID customerId) {
        return customerService.getCustomer(customerId);
    }

    @GetMapping("/email/{email}")
    public CustomerSummaryResponse getByEmail(@PathVariable String email) {
        return customerService.getSummaryByEmail(email);
    }

    @GetMapping("/external/{externalUserId}")
    public CustomerSummaryResponse getByExternalUserId(@PathVariable String externalUserId) {
        return customerService.getSummaryByExternalUserId(externalUserId);
    }

    @GetMapping("/{customerId}/summary")
    public CustomerSummaryResponse getSummary(@PathVariable UUID customerId) {
        return customerService.getSummary(customerId);
    }

    @GetMapping("/{customerId}/default-addresses")
    public CustomerDefaultAddressesResponse getDefaultAddresses(@PathVariable UUID customerId) {
        return customerAddressService.getDefaultAddresses(customerId);
    }
}
