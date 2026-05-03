package com.ascmoda.shared.security.auth;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class CurrentAuthentication {

    public String subject() {
        Authentication authentication = authentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuthenticationToken) {
            return jwtAuthenticationToken.getToken().getSubject();
        }
        if (authentication.getPrincipal() instanceof Jwt jwt) {
            return jwt.getSubject();
        }
        return authentication.getName();
    }

    public boolean hasAuthority(String authority) {
        return authorities().contains(authority);
    }

    public boolean hasAnyAuthority(String... authorities) {
        Set<String> currentAuthorities = authorities();
        return Arrays.stream(authorities).anyMatch(currentAuthorities::contains);
    }

    public void requireAnyAuthority(String... authorities) {
        if (!hasAnyAuthority(authorities)) {
            throw new AccessDeniedException("Access denied");
        }
    }

    private Authentication authentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Authentication is required");
        }
        return authentication;
    }

    private Set<String> authorities() {
        return authentication().getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
    }
}
