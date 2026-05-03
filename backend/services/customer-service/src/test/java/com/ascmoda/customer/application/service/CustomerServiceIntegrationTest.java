package com.ascmoda.customer.application.service;

import com.ascmoda.customer.controller.dto.ChangeCustomerStatusRequest;
import com.ascmoda.customer.controller.dto.CreateCustomerAddressRequest;
import com.ascmoda.customer.controller.dto.CreateCustomerRequest;
import com.ascmoda.customer.controller.dto.CustomerAddressResponse;
import com.ascmoda.customer.controller.dto.CustomerDefaultAddressesResponse;
import com.ascmoda.customer.controller.dto.CustomerResponse;
import com.ascmoda.customer.controller.dto.CustomerSummaryResponse;
import com.ascmoda.customer.controller.dto.UpdateCustomerAddressRequest;
import com.ascmoda.customer.controller.dto.UpdateCustomerProfileRequest;
import com.ascmoda.customer.domain.exception.BlockedCustomerOperationException;
import com.ascmoda.customer.domain.exception.CustomerAddressNotFoundException;
import com.ascmoda.customer.domain.exception.DuplicateEmailException;
import com.ascmoda.customer.domain.exception.DuplicateExternalUserIdException;
import com.ascmoda.customer.domain.exception.InactiveAddressOperationException;
import com.ascmoda.customer.domain.exception.InvalidCustomerStatusTransitionException;
import com.ascmoda.customer.domain.model.AddressType;
import com.ascmoda.customer.domain.model.CustomerStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
        "ascmoda.customer.config-source=test"
})
@Testcontainers(disabledWithoutDocker = true)
@Transactional
class CustomerServiceIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private CustomerService customerService;

    @Autowired
    private CustomerAddressService customerAddressService;

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> "true");
    }

    @Test
    void createsCustomerWithNormalizedEmail() {
        CustomerResponse customer = createCustomer("Ada@Example.COM", "+905551110000");

        assertThat(customer.id()).isNotNull();
        assertThat(customer.email()).isEqualTo("ada@example.com");
        assertThat(customer.status()).isEqualTo(CustomerStatus.ACTIVE);
        assertThat(customer.fullName()).isEqualTo("Ada Lovelace");
        assertThat(customer.addresses()).isEmpty();
    }

    @Test
    void rejectsDuplicateEmail() {
        createCustomer("duplicate@example.com", "+905551110001");

        assertThatThrownBy(() -> createCustomer("DUPLICATE@example.com", "+905551110002"))
                .isInstanceOf(DuplicateEmailException.class);
    }

    @Test
    void createWithSameExternalUserIdAndEmailReturnsExistingCustomer() {
        CustomerResponse first = createCustomer(
                "external-idempotent@example.com",
                "+905551110016",
                "keycloak-idempotent-1"
        );

        CustomerResponse second = createCustomer(
                "EXTERNAL-IDEMPOTENT@example.com",
                "+905551110017",
                "keycloak-idempotent-1"
        );

        assertThat(second.id()).isEqualTo(first.id());
        assertThat(second.externalUserId()).isEqualTo("keycloak-idempotent-1");
        assertThat(second.email()).isEqualTo("external-idempotent@example.com");
    }

    @Test
    void rejectsDuplicateExternalUserIdWithDifferentEmail() {
        createCustomer("external-duplicate@example.com", "+905551110018", "keycloak-duplicate-1");

        assertThatThrownBy(() -> createCustomer(
                "external-duplicate-other@example.com",
                "+905551110019",
                "keycloak-duplicate-1"
        )).isInstanceOf(DuplicateExternalUserIdException.class);
    }

    @Test
    void updatesCustomerProfile() {
        CustomerResponse customer = createCustomer("profile@example.com", "+905551110003");

        CustomerResponse updated = customerService.updateProfile(
                customer.id(),
                new UpdateCustomerProfileRequest(
                        "keycloak-user-1",
                        "updated-profile@example.com",
                        "+905551110004",
                        "Grace",
                        "Hopper",
                        true,
                        true,
                        true
                )
        );

        assertThat(updated.externalUserId()).isEqualTo("keycloak-user-1");
        assertThat(updated.email()).isEqualTo("updated-profile@example.com");
        assertThat(updated.phoneNumber()).isEqualTo("+905551110004");
        assertThat(updated.fullName()).isEqualTo("Grace Hopper");
        assertThat(updated.emailVerified()).isTrue();
        assertThat(updated.phoneVerified()).isTrue();
        assertThat(updated.marketingConsent()).isTrue();
    }

    @Test
    void blockedCustomerProfileUpdateIsRejected() {
        CustomerResponse customer = createCustomer("blocked-profile@example.com", "+905551110020");
        customerService.changeStatus(customer.id(), new ChangeCustomerStatusRequest(CustomerStatus.BLOCKED));

        assertThatThrownBy(() -> customerService.updateProfile(
                customer.id(),
                new UpdateCustomerProfileRequest(
                        null,
                        "blocked-profile-updated@example.com",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                )
        )).isInstanceOf(BlockedCustomerOperationException.class);
    }

    @Test
    void rejectsDirectBlockedToActiveStatusTransition() {
        CustomerResponse customer = createCustomer("blocked-transition@example.com", "+905551110021");
        customerService.changeStatus(customer.id(), new ChangeCustomerStatusRequest(CustomerStatus.BLOCKED));

        assertThatThrownBy(() -> customerService.changeStatus(
                customer.id(),
                new ChangeCustomerStatusRequest(CustomerStatus.ACTIVE)
        )).isInstanceOf(InvalidCustomerStatusTransitionException.class);
    }

    @Test
    void addsAndUpdatesCustomerAddress() {
        CustomerResponse customer = createCustomer("address@example.com", "+905551110005");

        CustomerAddressResponse address = customerAddressService.addAddress(
                customer.id(),
                addressRequest("Home", AddressType.SHIPPING, false, false)
        );

        assertThat(address.defaultShipping()).isTrue();
        assertThat(address.defaultBilling()).isFalse();

        CustomerAddressResponse updated = customerAddressService.updateAddress(
                customer.id(),
                address.id(),
                new UpdateCustomerAddressRequest(
                        "Office",
                        null,
                        "Ada Lovelace",
                        "+905551110006",
                        "Ankara",
                        "Cankaya",
                        "Updated address line",
                        "06000",
                        "Turkey",
                        null,
                        null
                )
        );

        assertThat(updated.title()).isEqualTo("Office");
        assertThat(updated.city()).isEqualTo("Ankara");
        assertThat(updated.district()).isEqualTo("Cankaya");
        assertThat(updated.defaultShipping()).isTrue();
    }

    @Test
    void blockedCustomerAddressCreateIsRejected() {
        CustomerResponse customer = createCustomer("blocked-address-create@example.com", "+905551110022");
        customerService.changeStatus(customer.id(), new ChangeCustomerStatusRequest(CustomerStatus.BLOCKED));

        assertThatThrownBy(() -> customerAddressService.addAddress(
                customer.id(),
                addressRequest("Blocked create", AddressType.SHIPPING, true, false)
        )).isInstanceOf(BlockedCustomerOperationException.class);
    }

    @Test
    void blockedCustomerAddressUpdateIsRejected() {
        CustomerResponse customer = createCustomer("blocked-address-update@example.com", "+905551110023");
        CustomerAddressResponse address = customerAddressService.addAddress(
                customer.id(),
                addressRequest("Blocked update", AddressType.SHIPPING, true, false)
        );
        customerService.changeStatus(customer.id(), new ChangeCustomerStatusRequest(CustomerStatus.BLOCKED));

        assertThatThrownBy(() -> customerAddressService.updateAddress(
                customer.id(),
                address.id(),
                new UpdateCustomerAddressRequest("Updated", null, null, null, null, null, null, null, null, null, null)
        )).isInstanceOf(BlockedCustomerOperationException.class);
    }

    @Test
    void blockedCustomerDefaultAddressSetIsRejected() {
        CustomerResponse customer = createCustomer("blocked-default@example.com", "+905551110024");
        CustomerAddressResponse address = customerAddressService.addAddress(
                customer.id(),
                addressRequest("Blocked default", AddressType.SHIPPING, true, false)
        );
        customerService.changeStatus(customer.id(), new ChangeCustomerStatusRequest(CustomerStatus.BLOCKED));

        assertThatThrownBy(() -> customerAddressService.setDefaultShippingAddress(customer.id(), address.id()))
                .isInstanceOf(BlockedCustomerOperationException.class);
    }

    @Test
    void changingDefaultShippingClearsPreviousDefault() {
        CustomerResponse customer = createCustomer("shipping@example.com", "+905551110007");
        CustomerAddressResponse first = customerAddressService.addAddress(
                customer.id(),
                addressRequest("First", AddressType.SHIPPING, true, false)
        );
        CustomerAddressResponse second = customerAddressService.addAddress(
                customer.id(),
                addressRequest("Second", AddressType.SHIPPING, true, false)
        );

        assertThat(customerAddressService.listAddresses(customer.id()))
                .filteredOn(CustomerAddressResponse::defaultShipping)
                .singleElement()
                .extracting(CustomerAddressResponse::id)
                .isEqualTo(second.id());

        customerAddressService.setDefaultShippingAddress(customer.id(), first.id());

        assertThat(customerAddressService.listAddresses(customer.id()))
                .filteredOn(CustomerAddressResponse::defaultShipping)
                .singleElement()
                .extracting(CustomerAddressResponse::id)
                .isEqualTo(first.id());
    }

    @Test
    void changingDefaultBillingClearsPreviousDefault() {
        CustomerResponse customer = createCustomer("billing@example.com", "+905551110008");
        CustomerAddressResponse first = customerAddressService.addAddress(
                customer.id(),
                addressRequest("First invoice", AddressType.BILLING, false, true)
        );
        CustomerAddressResponse second = customerAddressService.addAddress(
                customer.id(),
                addressRequest("Second invoice", AddressType.BILLING, false, true)
        );

        assertThat(customerAddressService.listAddresses(customer.id()))
                .filteredOn(CustomerAddressResponse::defaultBilling)
                .singleElement()
                .extracting(CustomerAddressResponse::id)
                .isEqualTo(second.id());

        customerAddressService.setDefaultBillingAddress(customer.id(), first.id());

        assertThat(customerAddressService.listAddresses(customer.id()))
                .filteredOn(CustomerAddressResponse::defaultBilling)
                .singleElement()
                .extracting(CustomerAddressResponse::id)
                .isEqualTo(first.id());
    }

    @Test
    void preventsOperatingOnAnotherCustomersAddress() {
        CustomerResponse owner = createCustomer("owner@example.com", "+905551110009");
        CustomerResponse other = createCustomer("other@example.com", "+905551110010");
        CustomerAddressResponse ownerAddress = customerAddressService.addAddress(
                owner.id(),
                addressRequest("Owner address", AddressType.SHIPPING, true, false)
        );

        assertThatThrownBy(() -> customerAddressService.updateAddress(
                other.id(),
                ownerAddress.id(),
                new UpdateCustomerAddressRequest("Hacked", null, null, null, null, null, null, null, null, null, null)
        )).isInstanceOf(CustomerAddressNotFoundException.class);
    }

    @Test
    void supportsAdminFilteringAndPagination() {
        CustomerResponse active = createCustomer("alice@example.com", "+905551110011", "admin-external-1");
        CustomerResponse passive = createCustomer("bob@example.com", "+905551110012");
        customerService.changeStatus(passive.id(), new ChangeCustomerStatusRequest(CustomerStatus.PASSIVE));

        Page<CustomerSummaryResponse> passivePage = customerService.listAdmin(
                CustomerStatus.PASSIVE,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                PageRequest.of(0, 10, Sort.by("createdAt").descending())
        );
        Page<CustomerSummaryResponse> emailPage = customerService.listAdmin(
                null,
                "ali",
                null,
                null,
                null,
                null,
                null,
                null,
                PageRequest.of(0, 1, Sort.by("createdAt").descending())
        );
        Page<CustomerSummaryResponse> nameAndExternalPage = customerService.listAdmin(
                null,
                null,
                null,
                "ada",
                "love",
                null,
                null,
                null,
                PageRequest.of(0, 20, Sort.by("createdAt").descending())
        );
        Page<CustomerSummaryResponse> externalPage = customerService.listAdmin(
                null,
                null,
                null,
                null,
                null,
                "admin-external",
                null,
                null,
                PageRequest.of(0, 10, Sort.by("createdAt").descending())
        );

        assertThat(passivePage.getTotalElements()).isEqualTo(1);
        assertThat(passivePage.getContent().get(0).email()).isEqualTo("bob@example.com");
        assertThat(emailPage.getTotalElements()).isEqualTo(1);
        assertThat(emailPage.getContent().get(0).customerId()).isEqualTo(active.id());
        assertThat(emailPage.getSize()).isEqualTo(1);
        assertThat(nameAndExternalPage.getTotalElements()).isGreaterThanOrEqualTo(2);
        assertThat(externalPage.getTotalElements()).isEqualTo(1);
        assertThat(externalPage.getContent().get(0).externalUserId()).isEqualTo("admin-external-1");
    }

    @Test
    void returnsInternalSummaryAndDefaultAddresses() {
        CustomerResponse customer = createCustomer("internal@example.com", "+905551110013", "keycloak-internal-1");
        CustomerAddressResponse shipping = customerAddressService.addAddress(
                customer.id(),
                addressRequest("Shipping", AddressType.SHIPPING, true, false)
        );
        CustomerAddressResponse billing = customerAddressService.addAddress(
                customer.id(),
                addressRequest("Billing", AddressType.BILLING, false, true)
        );

        CustomerSummaryResponse summary = customerService.getSummary(customer.id());
        CustomerDefaultAddressesResponse defaults = customerAddressService.getDefaultAddresses(customer.id());

        assertThat(summary.customerId()).isEqualTo(customer.id());
        assertThat(summary.externalUserId()).isEqualTo("keycloak-internal-1");
        assertThat(summary.fullName()).isEqualTo("Ada Lovelace");
        assertThat(summary.displayName()).isEqualTo("Ada Lovelace");
        assertThat(summary.emailVerified()).isFalse();
        assertThat(summary.phoneVerified()).isFalse();
        assertThat(summary.hasDefaultShippingAddress()).isTrue();
        assertThat(summary.hasDefaultBillingAddress()).isTrue();
        assertThat(summary.defaultShippingAddress().id()).isEqualTo(shipping.id());
        assertThat(summary.defaultBillingAddress().id()).isEqualTo(billing.id());
        assertThat(defaults.externalUserId()).isEqualTo("keycloak-internal-1");
        assertThat(defaults.fullName()).isEqualTo("Ada Lovelace");
        assertThat(defaults.hasActiveAddress()).isTrue();
        assertThat(defaults.hasDefaultShippingAddress()).isTrue();
        assertThat(defaults.hasDefaultBillingAddress()).isTrue();
        assertThat(defaults.defaultShippingAddress().id()).isEqualTo(shipping.id());
        assertThat(defaults.defaultBillingAddress().id()).isEqualTo(billing.id());
    }

    @Test
    void deactivatingDefaultAddressClearsDefaultFlags() {
        CustomerResponse customer = createCustomer("default-cleared@example.com", "+905551110025");
        CustomerAddressResponse address = customerAddressService.addAddress(
                customer.id(),
                addressRequest("Only address", AddressType.OTHER, true, true)
        );

        customerAddressService.deactivateAddress(customer.id(), address.id());
        CustomerDefaultAddressesResponse defaults = customerAddressService.getDefaultAddresses(customer.id());

        assertThat(defaults.hasActiveAddress()).isFalse();
        assertThat(defaults.hasDefaultShippingAddress()).isFalse();
        assertThat(defaults.hasDefaultBillingAddress()).isFalse();
        assertThat(defaults.defaultShippingAddress()).isNull();
        assertThat(defaults.defaultBillingAddress()).isNull();
    }

    @Test
    void inactiveAddressCannotBecomeDefault() {
        CustomerResponse customer = createCustomer("inactive@example.com", "+905551110014");
        CustomerAddressResponse address = customerAddressService.addAddress(
                customer.id(),
                addressRequest("Temporary", AddressType.SHIPPING, true, false)
        );
        customerAddressService.deactivateAddress(customer.id(), address.id());

        assertThatThrownBy(() -> customerAddressService.setDefaultShippingAddress(customer.id(), address.id()))
                .isInstanceOf(InactiveAddressOperationException.class);
    }

    private CustomerResponse createCustomer(String email, String phoneNumber) {
        return createCustomer(email, phoneNumber, null);
    }

    private CustomerResponse createCustomer(String email, String phoneNumber, String externalUserId) {
        return customerService.createCustomer(new CreateCustomerRequest(
                externalUserId,
                email,
                phoneNumber,
                "Ada",
                "Lovelace",
                false,
                false,
                false
        ));
    }

    private CreateCustomerAddressRequest addressRequest(String title, AddressType addressType,
                                                        boolean defaultShipping, boolean defaultBilling) {
        return new CreateCustomerAddressRequest(
                title,
                addressType,
                "Ada Lovelace",
                "+905551110015",
                "Istanbul",
                "Kadikoy",
                "Moda Caddesi No: 1",
                "34710",
                "Turkey",
                defaultShipping,
                defaultBilling
        );
    }
}
