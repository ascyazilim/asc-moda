package com.ascmoda.customer.security;

import com.ascmoda.customer.application.service.CustomerService;
import com.ascmoda.customer.controller.dto.CreateCustomerRequest;
import com.ascmoda.customer.controller.dto.CustomerResponse;
import com.ascmoda.shared.security.auth.SecurityAuthorities;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.hamcrest.Matchers.equalTo;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "debug=false",
        "spring.cloud.config.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "spring.cloud.service-registry.auto-registration.enabled=false",
        "spring.config.import=",
        "eureka.client.enabled=false",
        "eureka.client.register-with-eureka=false",
        "eureka.client.fetch-registry=false",
        "spring.jpa.open-in-view=false",
        "spring.security.oauth2.resourceserver.jwt.issuer-uri=http://issuer.test/realms/asc-moda",
        "ascmoda.customer.config-source=test"
})
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
@Transactional
class CustomerSecurityIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private CustomerService customerService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> "true");
    }

    @Test
    void ownerCanAccessOwnCustomerProfile() throws Exception {
        CustomerResponse customer = createCustomer("owner-profile@example.com", "owner-subject");

        mockMvc.perform(get("/api/v1/customers/{customerId}", customer.id())
                        .with(jwt().jwt(token -> token.subject("owner-subject"))
                                .authorities(new SimpleGrantedAuthority(SecurityAuthorities.ROLE_CUSTOMER))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", equalTo(customer.id().toString())));
    }

    @Test
    void customerCannotAccessAnotherCustomerProfile() throws Exception {
        CustomerResponse customer = createCustomer("other-profile@example.com", "profile-owner-subject");

        mockMvc.perform(get("/api/v1/customers/{customerId}", customer.id())
                        .with(jwt().jwt(token -> token.subject("different-subject"))
                                .authorities(new SimpleGrantedAuthority(SecurityAuthorities.ROLE_CUSTOMER))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode", equalTo("FORBIDDEN")));
    }

    @Test
    void adminCanAccessAnyCustomerProfile() throws Exception {
        CustomerResponse customer = createCustomer("admin-profile@example.com", "admin-owned-subject");

        mockMvc.perform(get("/api/v1/customers/{customerId}", customer.id())
                        .with(jwt().jwt(token -> token.subject("admin-subject"))
                                .authorities(new SimpleGrantedAuthority(SecurityAuthorities.ROLE_ADMIN))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", equalTo(customer.id().toString())));
    }

    @Test
    void internalEndpointRejectsCustomerTokenAndAcceptsServiceToken() throws Exception {
        CustomerResponse customer = createCustomer("internal-profile@example.com", "internal-owner-subject");

        mockMvc.perform(get("/api/v1/internal/customers/{customerId}/summary", customer.id())
                        .with(jwt().jwt(token -> token.subject("internal-owner-subject"))
                                .authorities(new SimpleGrantedAuthority(SecurityAuthorities.ROLE_CUSTOMER))))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/internal/customers/{customerId}/summary", customer.id())
                        .with(jwt().jwt(token -> token.subject("cart-service"))
                                .authorities(new SimpleGrantedAuthority(SecurityAuthorities.ROLE_SERVICE))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId", equalTo(customer.id().toString())));
    }

    @Test
    void customerCreateUsesTokenSubjectAsExternalUserId() throws Exception {
        CreateCustomerRequest request = new CreateCustomerRequest(
                null,
                "create-owner@example.com",
                "+905551119999",
                "Ada",
                "Lovelace",
                false,
                false,
                false
        );

        mockMvc.perform(post("/api/v1/customers")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request))
                        .with(jwt().jwt(token -> token.subject("create-owner-subject"))
                                .authorities(new SimpleGrantedAuthority(SecurityAuthorities.ROLE_CUSTOMER))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.externalUserId", equalTo("create-owner-subject")));
    }

    private CustomerResponse createCustomer(String email, String externalUserId) {
        return customerService.createCustomer(new CreateCustomerRequest(
                externalUserId,
                email,
                "+905551110000",
                "Ada",
                "Lovelace",
                false,
                false,
                false
        ));
    }
}
