package com.ascmoda.shared.security.auth;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class KeycloakJwtAuthoritiesConverterTest {

    private final KeycloakJwtAuthoritiesConverter converter = new KeycloakJwtAuthoritiesConverter();

    @Test
    void mapsRealmRolesClientRolesAndScopes() {
        Jwt jwt = new Jwt(
                "token",
                Instant.now(),
                Instant.now().plusSeconds(300),
                Map.of("alg", "none"),
                Map.of(
                        "sub", "user-1",
                        "realm_access", Map.of("roles", List.of("ROLE_CUSTOMER", "ROLE_ADMIN")),
                        "resource_access", Map.of(
                                "customer-service", Map.of("roles", List.of("customer:read", "customer:update")),
                                "order-service", Map.of("roles", List.of("order:create"))
                        ),
                        "scope", "openid profile"
                )
        );

        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        assertThat(authorities)
                .extracting(GrantedAuthority::getAuthority)
                .contains(
                        "ROLE_CUSTOMER",
                        "ROLE_ADMIN",
                        "customer:read",
                        "customer:update",
                        "order:create",
                        "SCOPE_openid",
                        "SCOPE_profile"
                );
    }
}
