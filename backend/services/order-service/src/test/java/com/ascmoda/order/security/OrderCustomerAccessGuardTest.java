package com.ascmoda.order.security;

import com.ascmoda.order.infrastructure.customer.CustomerIdentityClient;
import com.ascmoda.order.infrastructure.customer.CustomerIdentityResponse;
import com.ascmoda.shared.security.auth.CurrentAuthentication;
import com.ascmoda.shared.security.auth.SecurityAuthorities;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class OrderCustomerAccessGuardTest {

    private final CurrentAuthentication currentAuthentication = mock(CurrentAuthentication.class);
    private final CustomerIdentityClient customerIdentityClient = mock(CustomerIdentityClient.class);
    private final OrderCustomerAccessGuard guard = new OrderCustomerAccessGuard(
            currentAuthentication,
            customerIdentityClient
    );

    @Test
    void ownerCanAccessCustomerOrder() {
        UUID customerId = UUID.randomUUID();
        when(currentAuthentication.hasAnyAuthority(
                SecurityAuthorities.ROLE_ADMIN,
                SecurityAuthorities.ROLE_SUPPORT,
                SecurityAuthorities.ROLE_OPERATIONS,
                SecurityAuthorities.ROLE_SERVICE
        )).thenReturn(false);
        when(currentAuthentication.subject()).thenReturn("keycloak-user-1");
        when(customerIdentityClient.getSummary(customerId))
                .thenReturn(new CustomerIdentityResponse(customerId, "keycloak-user-1"));

        assertThatNoException().isThrownBy(() -> guard.assertCanAccessCustomer(customerId));
    }

    @Test
    void differentCustomerIsRejected() {
        UUID customerId = UUID.randomUUID();
        when(currentAuthentication.hasAnyAuthority(
                SecurityAuthorities.ROLE_ADMIN,
                SecurityAuthorities.ROLE_SUPPORT,
                SecurityAuthorities.ROLE_OPERATIONS,
                SecurityAuthorities.ROLE_SERVICE
        )).thenReturn(false);
        when(currentAuthentication.subject()).thenReturn("keycloak-user-2");
        when(customerIdentityClient.getSummary(customerId))
                .thenReturn(new CustomerIdentityResponse(customerId, "keycloak-user-1"));

        assertThatThrownBy(() -> guard.assertCanAccessCustomer(customerId))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void privilegedAuthorityBypassesOwnershipLookup() {
        UUID customerId = UUID.randomUUID();
        when(currentAuthentication.hasAnyAuthority(
                SecurityAuthorities.ROLE_ADMIN,
                SecurityAuthorities.ROLE_SUPPORT,
                SecurityAuthorities.ROLE_OPERATIONS,
                SecurityAuthorities.ROLE_SERVICE
        )).thenReturn(true);

        assertThatNoException().isThrownBy(() -> guard.assertCanAccessCustomer(customerId));
        verifyNoInteractions(customerIdentityClient);
    }
}
