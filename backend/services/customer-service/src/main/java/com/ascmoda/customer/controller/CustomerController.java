package com.ascmoda.customer.controller;

import com.ascmoda.customer.application.service.CustomerAddressService;
import com.ascmoda.customer.application.service.CustomerService;
import com.ascmoda.customer.controller.dto.CreateCustomerAddressRequest;
import com.ascmoda.customer.controller.dto.CreateCustomerRequest;
import com.ascmoda.customer.controller.dto.CustomerAddressResponse;
import com.ascmoda.customer.controller.dto.CustomerResponse;
import com.ascmoda.customer.controller.dto.CustomerSummaryResponse;
import com.ascmoda.customer.controller.dto.UpdateCustomerAddressRequest;
import com.ascmoda.customer.controller.dto.UpdateCustomerProfileRequest;
import com.ascmoda.customer.security.CustomerAccessGuard;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/customers")
public class CustomerController {

    private final CustomerService customerService;
    private final CustomerAddressService customerAddressService;
    private final CustomerAccessGuard customerAccessGuard;

    public CustomerController(CustomerService customerService, CustomerAddressService customerAddressService,
                              CustomerAccessGuard customerAccessGuard) {
        this.customerService = customerService;
        this.customerAddressService = customerAddressService;
        this.customerAccessGuard = customerAccessGuard;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerResponse create(@Valid @RequestBody CreateCustomerRequest request) {
        return customerService.createCustomer(customerAccessGuard.resolveCreateRequest(request));
    }

    @GetMapping("/{customerId}")
    public CustomerResponse get(@PathVariable UUID customerId) {
        customerAccessGuard.assertCanAccessCustomer(customerId);
        return customerService.getCustomer(customerId);
    }

    @PatchMapping("/{customerId}")
    public CustomerResponse updateProfile(@PathVariable UUID customerId,
                                          @Valid @RequestBody UpdateCustomerProfileRequest request) {
        customerAccessGuard.assertCanAccessCustomer(customerId);
        return customerService.updateProfile(customerId, request);
    }

    @GetMapping("/{customerId}/summary")
    public CustomerSummaryResponse getSummary(@PathVariable UUID customerId) {
        customerAccessGuard.assertCanAccessCustomer(customerId);
        return customerService.getSummary(customerId);
    }

    @GetMapping("/{customerId}/addresses")
    public List<CustomerAddressResponse> listAddresses(@PathVariable UUID customerId) {
        customerAccessGuard.assertCanAccessCustomer(customerId);
        return customerAddressService.listAddresses(customerId);
    }

    @PostMapping("/{customerId}/addresses")
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerAddressResponse addAddress(@PathVariable UUID customerId,
                                              @Valid @RequestBody CreateCustomerAddressRequest request) {
        customerAccessGuard.assertCanAccessCustomer(customerId);
        return customerAddressService.addAddress(customerId, request);
    }

    @PatchMapping("/{customerId}/addresses/{addressId}")
    public CustomerAddressResponse updateAddress(@PathVariable UUID customerId,
                                                 @PathVariable UUID addressId,
                                                 @Valid @RequestBody UpdateCustomerAddressRequest request) {
        customerAccessGuard.assertCanAccessCustomer(customerId);
        return customerAddressService.updateAddress(customerId, addressId, request);
    }

    @DeleteMapping("/{customerId}/addresses/{addressId}")
    public CustomerAddressResponse deactivateAddress(@PathVariable UUID customerId, @PathVariable UUID addressId) {
        customerAccessGuard.assertCanAccessCustomer(customerId);
        return customerAddressService.deactivateAddress(customerId, addressId);
    }

    @PatchMapping("/{customerId}/addresses/{addressId}/default-shipping")
    public CustomerAddressResponse setDefaultShipping(@PathVariable UUID customerId, @PathVariable UUID addressId) {
        customerAccessGuard.assertCanAccessCustomer(customerId);
        return customerAddressService.setDefaultShippingAddress(customerId, addressId);
    }

    @PatchMapping("/{customerId}/addresses/{addressId}/default-billing")
    public CustomerAddressResponse setDefaultBilling(@PathVariable UUID customerId, @PathVariable UUID addressId) {
        customerAccessGuard.assertCanAccessCustomer(customerId);
        return customerAddressService.setDefaultBillingAddress(customerId, addressId);
    }
}
