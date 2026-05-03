package com.ascmoda.shared.security.feign;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class InternalFeignOAuth2AutoConfigurationTest {

    @Test
    void addsBearerTokenToFeignRequest() {
        ClientRegistration registration = ClientRegistration
                .withRegistrationId("ascmoda-service")
                .tokenUri("http://keycloak/token")
                .clientId("cart-service")
                .clientSecret("secret")
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .build();
        OAuth2AccessToken accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                "service-token",
                Instant.now(),
                Instant.now().plusSeconds(300)
        );
        OAuth2AuthorizedClient client = new OAuth2AuthorizedClient(registration, "cart-service", accessToken);
        OAuth2AuthorizedClientManager manager = request -> client;
        RequestInterceptor interceptor = new InternalFeignOAuth2AutoConfiguration()
                .ascModaServiceTokenRequestInterceptor(manager, "ascmoda-service");

        RequestTemplate template = new RequestTemplate();
        interceptor.apply(template);

        assertThat(template.headers())
                .containsEntry("Authorization", java.util.List.of("Bearer service-token"));
    }
}
