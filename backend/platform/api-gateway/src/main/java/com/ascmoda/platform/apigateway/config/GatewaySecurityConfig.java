package com.ascmoda.platform.apigateway.config;

import com.ascmoda.shared.security.auth.AscModaJwtAuthenticationConverters;
import com.ascmoda.shared.security.auth.SecurityAuthorities;
import com.ascmoda.shared.security.web.SecurityProblemSupport;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class GatewaySecurityConfig {

    @Bean
    @ConditionalOnProperty(name = "spring.security.oauth2.resourceserver.jwt.issuer-uri")
    SecurityWebFilterChain gatewaySecurityFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint(SecurityProblemSupport.reactiveAuthenticationEntryPoint())
                        .accessDeniedHandler(SecurityProblemSupport.reactiveAccessDeniedHandler())
                )
                .authorizeExchange(authorize -> authorize
                        .pathMatchers("/actuator/health", "/actuator/info").permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/v1/catalog/**").permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/v1/search/**").permitAll()
                        .pathMatchers("/api/v1/internal/**").hasAuthority(SecurityAuthorities.ROLE_SERVICE)
                        .pathMatchers("/api/v1/admin/**").hasAnyAuthority(
                                SecurityAuthorities.ROLE_ADMIN,
                                SecurityAuthorities.ROLE_SUPPORT,
                                SecurityAuthorities.ROLE_OPERATIONS
                        )
                        .pathMatchers("/api/v1/customers/**").authenticated()
                        .pathMatchers("/api/v1/carts/**").authenticated()
                        .pathMatchers("/api/v1/orders/**").authenticated()
                        .anyExchange().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(AscModaJwtAuthenticationConverters.reactive()))
                )
                .build();
    }

    @Bean
    @ConditionalOnMissingBean(SecurityWebFilterChain.class)
    SecurityWebFilterChain localGatewaySecurityFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(authorize -> authorize.anyExchange().permitAll())
                .build();
    }
}
