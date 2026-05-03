package com.ascmoda.shared.security.web;

import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

import java.util.Arrays;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public final class ServletAuthorityAuthorizationManagers {

    private ServletAuthorityAuthorizationManagers() {
    }

    public static AuthorizationManager<RequestAuthorizationContext> hasAll(String... authorities) {
        return (authentication, context) -> new AuthorizationDecision(hasAll(authentication, authorities));
    }

    public static AuthorizationManager<RequestAuthorizationContext> hasAny(String... authorities) {
        return (authentication, context) -> new AuthorizationDecision(hasAny(authentication, authorities));
    }

    public static AuthorizationManager<RequestAuthorizationContext> hasAnyAndAll(String[] anyAuthorities,
                                                                                 String... requiredAuthorities) {
        return (authentication, context) -> new AuthorizationDecision(
                hasAny(authentication, anyAuthorities) && hasAll(authentication, requiredAuthorities)
        );
    }

    private static boolean hasAll(Supplier<Authentication> supplier, String... authorities) {
        Set<String> currentAuthorities = authorities(supplier);
        return Arrays.stream(authorities).allMatch(currentAuthorities::contains);
    }

    private static boolean hasAny(Supplier<Authentication> supplier, String... authorities) {
        Set<String> currentAuthorities = authorities(supplier);
        return Arrays.stream(authorities).anyMatch(currentAuthorities::contains);
    }

    private static Set<String> authorities(Supplier<Authentication> supplier) {
        Authentication authentication = supplier.get();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Set.of();
        }
        return authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
    }
}
