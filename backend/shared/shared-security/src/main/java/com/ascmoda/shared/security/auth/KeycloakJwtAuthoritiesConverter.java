package com.ascmoda.shared.security.auth;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class KeycloakJwtAuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Set<GrantedAuthority> authorities = new LinkedHashSet<>();
        addRealmRoles(jwt, authorities);
        addClientRoles(jwt, authorities);
        addScopes(jwt, authorities);
        return authorities;
    }

    @SuppressWarnings("unchecked")
    private void addRealmRoles(Jwt jwt, Set<GrantedAuthority> authorities) {
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess == null) {
            return;
        }
        Object roles = realmAccess.get("roles");
        if (roles instanceof Collection<?> roleCollection) {
            roleCollection.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .forEach(role -> authorities.add(new SimpleGrantedAuthority(role)));
        }
    }

    private void addClientRoles(Jwt jwt, Set<GrantedAuthority> authorities) {
        Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
        if (resourceAccess == null) {
            return;
        }
        for (Object access : resourceAccess.values()) {
            if (!(access instanceof Map<?, ?> accessMap)) {
                continue;
            }
            Object roles = accessMap.get("roles");
            if (!(roles instanceof Collection<?> roleCollection)) {
                continue;
            }
            roleCollection.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .forEach(role -> authorities.add(new SimpleGrantedAuthority(role)));
        }
    }

    private void addScopes(Jwt jwt, Set<GrantedAuthority> authorities) {
        String scope = jwt.getClaimAsString("scope");
        if (scope == null || scope.isBlank()) {
            return;
        }
        for (String item : scope.split(" ")) {
            if (!item.isBlank()) {
                authorities.add(new SimpleGrantedAuthority("SCOPE_" + item));
            }
        }
    }
}
