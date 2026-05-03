package com.ascmoda.shared.security.feign;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

@AutoConfiguration
@ConditionalOnClass(RequestInterceptor.class)
public class InternalFeignOAuth2AutoConfiguration {

    @Bean
    @ConditionalOnBean({ClientRegistrationRepository.class, OAuth2AuthorizedClientService.class})
    OAuth2AuthorizedClientManager ascModaOAuth2AuthorizedClientManager(
            ClientRegistrationRepository clientRegistrationRepository,
            OAuth2AuthorizedClientService authorizedClientService
    ) {
        OAuth2AuthorizedClientProvider provider = OAuth2AuthorizedClientProviderBuilder.builder()
                .clientCredentials()
                .build();
        AuthorizedClientServiceOAuth2AuthorizedClientManager manager =
                new AuthorizedClientServiceOAuth2AuthorizedClientManager(
                        clientRegistrationRepository,
                        authorizedClientService
                );
        manager.setAuthorizedClientProvider(provider);
        return manager;
    }

    @Bean
    @ConditionalOnBean(OAuth2AuthorizedClientManager.class)
    RequestInterceptor ascModaServiceTokenRequestInterceptor(
            OAuth2AuthorizedClientManager authorizedClientManager,
            @Value("${ascmoda.security.service-client.registration-id:ascmoda-service}") String registrationId
    ) {
        return template -> {
            OAuth2AuthorizeRequest request = OAuth2AuthorizeRequest
                    .withClientRegistrationId(registrationId)
                    .principal("ascmoda-internal-service")
                    .build();
            var client = authorizedClientManager.authorize(request);
            if (client != null && client.getAccessToken() != null) {
                template.header(HttpHeaders.AUTHORIZATION, "Bearer " + client.getAccessToken().getTokenValue());
            }
        };
    }
}
