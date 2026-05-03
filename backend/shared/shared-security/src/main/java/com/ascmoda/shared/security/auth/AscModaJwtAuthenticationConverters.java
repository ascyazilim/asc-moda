package com.ascmoda.shared.security.auth;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import reactor.core.publisher.Mono;

public final class AscModaJwtAuthenticationConverters {

    private AscModaJwtAuthenticationConverters() {
    }

    public static JwtAuthenticationConverter servlet() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new KeycloakJwtAuthoritiesConverter());
        converter.setPrincipalClaimName("sub");
        return converter;
    }

    public static Converter<Jwt, Mono<AbstractAuthenticationToken>> reactive() {
        return new ReactiveJwtAuthenticationConverterAdapter(servlet());
    }
}
