package com.ascmoda.platform.apigateway.config;

import com.ascmoda.shared.security.auth.SecurityAuthorities;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt;
import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@SpringBootTest(properties = {
        "debug=false",
        "spring.cloud.config.enabled=false",
        "spring.config.import=",
        "eureka.client.enabled=false",
        "spring.security.oauth2.resourceserver.jwt.issuer-uri=http://issuer.test/realms/asc-moda",
        "spring.cloud.gateway.server.webflux.routes[0].id=test-public-catalog",
        "spring.cloud.gateway.server.webflux.routes[0].uri=forward:/test-ok",
        "spring.cloud.gateway.server.webflux.routes[0].predicates[0]=Path=/api/v1/catalog/**",
        "spring.cloud.gateway.server.webflux.routes[1].id=test-admin",
        "spring.cloud.gateway.server.webflux.routes[1].uri=forward:/test-ok",
        "spring.cloud.gateway.server.webflux.routes[1].predicates[0]=Path=/api/v1/admin/**",
        "spring.cloud.gateway.server.webflux.routes[2].id=test-internal",
        "spring.cloud.gateway.server.webflux.routes[2].uri=forward:/test-ok",
        "spring.cloud.gateway.server.webflux.routes[2].predicates[0]=Path=/api/v1/internal/**"
})
@AutoConfigureWebTestClient
class GatewaySecurityIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private ReactiveJwtDecoder reactiveJwtDecoder;

    @Test
    void publicCatalogReadIsAccessibleWithoutToken() {
        webTestClient.get()
                .uri("/api/v1/catalog/products")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void adminPathRequiresAdminLikeAuthority() {
        webTestClient.get()
                .uri("/api/v1/admin/customers")
                .exchange()
                .expectStatus().isUnauthorized();

        webTestClient.mutateWith(mockJwt().authorities(new SimpleGrantedAuthority(SecurityAuthorities.ROLE_CUSTOMER)))
                .get()
                .uri("/api/v1/admin/customers")
                .exchange()
                .expectStatus().isForbidden();

        webTestClient.mutateWith(mockJwt().authorities(new SimpleGrantedAuthority(SecurityAuthorities.ROLE_ADMIN)))
                .get()
                .uri("/api/v1/admin/customers")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void internalPathRequiresServiceAuthority() {
        webTestClient.mutateWith(mockJwt().authorities(new SimpleGrantedAuthority(SecurityAuthorities.ROLE_CUSTOMER)))
                .get()
                .uri("/api/v1/internal/customers/123")
                .exchange()
                .expectStatus().isForbidden();

        webTestClient.mutateWith(mockJwt().authorities(new SimpleGrantedAuthority(SecurityAuthorities.ROLE_SERVICE)))
                .get()
                .uri("/api/v1/internal/customers/123")
                .exchange()
                .expectStatus().isOk();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestRoutes {

        @Bean
        RouterFunction<ServerResponse> testOkRoute() {
            return route(GET("/test-ok"), request -> ServerResponse.ok().build());
        }
    }
}
