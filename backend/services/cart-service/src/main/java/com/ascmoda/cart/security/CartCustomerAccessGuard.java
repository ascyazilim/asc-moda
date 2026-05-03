package com.ascmoda.cart.security;

import com.ascmoda.cart.infrastructure.customer.CustomerIdentityClient;
import com.ascmoda.shared.security.auth.CurrentAuthentication;
import com.ascmoda.shared.security.auth.SecurityAuthorities;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class CartCustomerAccessGuard {

    private final CurrentAuthentication currentAuthentication;
    private final CustomerIdentityClient customerIdentityClient;

    public CartCustomerAccessGuard(CurrentAuthentication currentAuthentication,
                                   CustomerIdentityClient customerIdentityClient) {
        this.currentAuthentication = currentAuthentication;
        this.customerIdentityClient = customerIdentityClient;
    }

    public void assertCanAccessCustomer(UUID customerId) {
        if (hasPrivilegedAccess()) {
            return;
        }
        String subject = currentAuthentication.subject();
        String externalUserId = customerIdentityClient.getSummary(customerId).externalUserId();
        if (!subject.equals(externalUserId)) {
            throw new AccessDeniedException("Customer ownership validation failed");
        }
    }

    private boolean hasPrivilegedAccess() {
        return currentAuthentication.hasAnyAuthority(
                SecurityAuthorities.ROLE_ADMIN,
                SecurityAuthorities.ROLE_SUPPORT,
                SecurityAuthorities.ROLE_OPERATIONS,
                SecurityAuthorities.ROLE_SERVICE
        );
    }
}
