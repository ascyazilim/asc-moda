package com.ascmoda.shared.security.auth;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class AscModaSecurityAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    CurrentAuthentication currentAuthentication() {
        return new CurrentAuthentication();
    }
}
