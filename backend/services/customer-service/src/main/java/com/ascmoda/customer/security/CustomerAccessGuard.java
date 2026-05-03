package com.ascmoda.customer.security;

import com.ascmoda.customer.controller.dto.CreateCustomerRequest;
import com.ascmoda.customer.domain.exception.CustomerNotFoundException;
import com.ascmoda.customer.domain.model.Customer;
import com.ascmoda.customer.domain.repository.CustomerRepository;
import com.ascmoda.shared.security.auth.CurrentAuthentication;
import com.ascmoda.shared.security.auth.SecurityAuthorities;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class CustomerAccessGuard {

    private final CustomerRepository customerRepository;
    private final CurrentAuthentication currentAuthentication;

    public CustomerAccessGuard(CustomerRepository customerRepository, CurrentAuthentication currentAuthentication) {
        this.customerRepository = customerRepository;
        this.currentAuthentication = currentAuthentication;
    }

    public void assertCanAccessCustomer(UUID customerId) {
        if (isPrivileged()) {
            return;
        }

        String subject = currentAuthentication.subject();
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found: " + customerId));
        if (customer.getExternalUserId() == null || !customer.getExternalUserId().equals(subject)) {
            throw new AccessDeniedException("Customer ownership check failed");
        }
    }

    public CreateCustomerRequest resolveCreateRequest(CreateCustomerRequest request) {
        if (isPrivileged()) {
            return request;
        }

        String subject = currentAuthentication.subject();
        if (request.externalUserId() != null && !request.externalUserId().equals(subject)) {
            throw new AccessDeniedException("Customer external user id must match token subject");
        }

        return new CreateCustomerRequest(
                subject,
                request.email(),
                request.phoneNumber(),
                request.firstName(),
                request.lastName(),
                request.emailVerified(),
                request.phoneVerified(),
                request.marketingConsent()
        );
    }

    private boolean isPrivileged() {
        return currentAuthentication.hasAnyAuthority(
                SecurityAuthorities.ROLE_ADMIN,
                SecurityAuthorities.ROLE_SUPPORT,
                SecurityAuthorities.ROLE_OPERATIONS,
                SecurityAuthorities.ROLE_SERVICE
        );
    }
}
