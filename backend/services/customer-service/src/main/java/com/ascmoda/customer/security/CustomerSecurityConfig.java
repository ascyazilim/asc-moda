package com.ascmoda.customer.security;

import com.ascmoda.shared.security.auth.AscModaJwtAuthenticationConverters;
import com.ascmoda.shared.security.auth.SecurityAuthorities;
import com.ascmoda.shared.security.web.SecurityProblemSupport;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class CustomerSecurityConfig {

    @Bean
    @ConditionalOnProperty(name = "spring.security.oauth2.resourceserver.jwt.issuer-uri")
    SecurityFilterChain customerSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint(SecurityProblemSupport.servletAuthenticationEntryPoint())
                        .accessDeniedHandler(SecurityProblemSupport.servletAccessDeniedHandler())
                )
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/api/v1/internal/customers/**").hasAuthority(SecurityAuthorities.ROLE_SERVICE)
                        .requestMatchers("/api/v1/admin/customers", "/api/v1/admin/customers/**").hasAnyAuthority(
                                SecurityAuthorities.ROLE_ADMIN,
                                SecurityAuthorities.ROLE_SUPPORT,
                                SecurityAuthorities.ROLE_OPERATIONS
                        )
                        .requestMatchers(HttpMethod.POST, "/api/v1/customers").hasAnyAuthority(
                                SecurityAuthorities.ROLE_CUSTOMER,
                                SecurityAuthorities.ROLE_ADMIN,
                                SecurityAuthorities.ROLE_SERVICE
                        )
                        .requestMatchers("/api/v1/customers/**").authenticated()
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(AscModaJwtAuthenticationConverters.servlet()))
                )
                .build();
    }

    @Bean
    @ConditionalOnMissingBean(SecurityFilterChain.class)
    SecurityFilterChain localCustomerSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
                .build();
    }
}
