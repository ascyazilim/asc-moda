package com.ascmoda.notification.security;

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
public class NotificationSecurityConfig {

    @Bean
    @ConditionalOnProperty(name = "spring.security.oauth2.resourceserver.jwt.issuer-uri")
    SecurityFilterChain notificationSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint(SecurityProblemSupport.servletAuthenticationEntryPoint())
                        .accessDeniedHandler(SecurityProblemSupport.servletAccessDeniedHandler())
                )
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/api/v1/internal/notifications/**").hasAuthority(SecurityAuthorities.ROLE_SERVICE)
                        .requestMatchers("/api/v1/admin/notifications/*/retry")
                        .access(ServletAuthorityAuthorizationManagers.hasAll(
                                SecurityAuthorities.ROLE_ADMIN,
                                SecurityAuthorities.NOTIFICATION_RETRY
                        ))
                        .requestMatchers("/api/v1/admin/notifications/**", "/api/v1/admin/notifications")
                        .hasAnyAuthority(SecurityAuthorities.ROLE_ADMIN, SecurityAuthorities.ROLE_SUPPORT)
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(AscModaJwtAuthenticationConverters.servlet()))
                )
                .build();
    }

    @Bean
    @ConditionalOnMissingBean(SecurityFilterChain.class)
    SecurityFilterChain localNotificationSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
                .build();
    }
}
