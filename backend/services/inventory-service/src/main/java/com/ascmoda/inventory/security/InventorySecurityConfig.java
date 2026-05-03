package com.ascmoda.inventory.security;

import com.ascmoda.shared.security.auth.AscModaJwtAuthenticationConverters;
import com.ascmoda.shared.security.auth.SecurityAuthorities;
import com.ascmoda.shared.security.web.SecurityProblemSupport;
import com.ascmoda.shared.security.web.ServletAuthorityAuthorizationManagers;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class InventorySecurityConfig {

    @Bean
    @ConditionalOnProperty(name = "spring.security.oauth2.resourceserver.jwt.issuer-uri")
    SecurityFilterChain inventorySecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint(SecurityProblemSupport.servletAuthenticationEntryPoint())
                        .accessDeniedHandler(SecurityProblemSupport.servletAccessDeniedHandler())
                )
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/api/v1/internal/inventory/reserve")
                        .access(ServletAuthorityAuthorizationManagers.hasAll(
                                SecurityAuthorities.ROLE_SERVICE,
                                SecurityAuthorities.INVENTORY_RESERVE
                        ))
                        .requestMatchers("/api/v1/internal/inventory/consume")
                        .access(ServletAuthorityAuthorizationManagers.hasAll(
                                SecurityAuthorities.ROLE_SERVICE,
                                SecurityAuthorities.INVENTORY_CONSUME
                        ))
                        .requestMatchers("/api/v1/internal/inventory/**").hasAuthority(SecurityAuthorities.ROLE_SERVICE)
                        .requestMatchers("/api/v1/admin/inventory/adjust")
                        .access(ServletAuthorityAuthorizationManagers.hasAnyAndAll(
                                new String[] {
                                        SecurityAuthorities.ROLE_ADMIN,
                                        SecurityAuthorities.ROLE_OPERATIONS
                                },
                                SecurityAuthorities.INVENTORY_ADJUST
                        ))
                        .requestMatchers("/api/v1/admin/inventory/**", "/api/v1/admin/inventory").hasAnyAuthority(
                                SecurityAuthorities.ROLE_ADMIN,
                                SecurityAuthorities.ROLE_OPERATIONS
                        )
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(AscModaJwtAuthenticationConverters.servlet()))
                )
                .build();
    }

    @Bean
    @ConditionalOnMissingBean(SecurityFilterChain.class)
    SecurityFilterChain localInventorySecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
                .build();
    }
}
