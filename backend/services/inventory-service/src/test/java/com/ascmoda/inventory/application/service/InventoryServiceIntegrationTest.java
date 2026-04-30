package com.ascmoda.inventory.application.service;

import com.ascmoda.inventory.api.dto.AdjustStockRequest;
import com.ascmoda.inventory.api.dto.AvailabilityResponse;
import com.ascmoda.inventory.api.dto.ConsumeStockReservationRequest;
import com.ascmoda.inventory.api.dto.CreateInventoryItemRequest;
import com.ascmoda.inventory.api.dto.InventoryItemResponse;
import com.ascmoda.inventory.api.dto.ReleaseStockRequest;
import com.ascmoda.inventory.api.dto.ReserveStockRequest;
import com.ascmoda.inventory.api.dto.StockReservationResponse;
import com.ascmoda.inventory.api.dto.ValidateStockRequest;
import com.ascmoda.inventory.api.error.DuplicateInventoryItemException;
import com.ascmoda.inventory.api.error.ExternalServiceUnavailableException;
import com.ascmoda.inventory.api.error.InvalidReservationStateException;
import com.ascmoda.inventory.api.error.InvalidStockStateException;
import com.ascmoda.inventory.domain.model.ReferenceType;
import com.ascmoda.inventory.domain.model.StockMovementType;
import com.ascmoda.inventory.domain.model.StockReservationStatus;
import com.ascmoda.inventory.infrastructure.catalog.CatalogVariantClient;
import com.ascmoda.inventory.infrastructure.catalog.CatalogVariantResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

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
        "ascmoda.inventory.config-source=test"
})
@Testcontainers(disabledWithoutDocker = true)
@Transactional
class InventoryServiceIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private InventoryService inventoryService;

    @MockitoBean
    private CatalogVariantClient catalogVariantClient;

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> "true");
    }

    @Test
    void createsInventoryItemWithLowStockMetadata() {
        UUID variantId = UUID.randomUUID();
        mockCatalogVariant(variantId, "SKU-CREATE-1");

        InventoryItemResponse item = inventoryService.create(createRequest(variantId, "SKU-CREATE-1", 12, 4));

        assertThat(item.productVariantId()).isEqualTo(variantId);
        assertThat(item.sku()).isEqualTo("SKU-CREATE-1");
        assertThat(item.quantityOnHand()).isEqualTo(12);
        assertThat(item.availableQuantity()).isEqualTo(12);
        assertThat(item.lowStockThreshold()).isEqualTo(4);
        assertThat(item.lowStock()).isFalse();
        assertThat(item.lastStockChangeAt()).isNotNull();
    }

    @Test
    void rejectsDuplicateSkuAndProductVariant() {
        UUID variantId = UUID.randomUUID();
        mockCatalogVariant(variantId, "SKU-DUP-1");
        inventoryService.create(createRequest(variantId, "SKU-DUP-1", 3, 5));

        UUID secondVariantId = UUID.randomUUID();
        mockCatalogVariant(secondVariantId, "SKU-DUP-1");
        assertThatThrownBy(() -> inventoryService.create(createRequest(secondVariantId, "SKU-DUP-1", 2, 5)))
                .isInstanceOf(DuplicateInventoryItemException.class);

        UUID reusedVariantId = UUID.randomUUID();
        when(catalogVariantClient.getVariant(reusedVariantId))
                .thenReturn(catalogVariant(reusedVariantId, "SKU-VARIANT-1"))
                .thenReturn(catalogVariant(reusedVariantId, "SKU-VARIANT-2"));
        inventoryService.create(createRequest(reusedVariantId, "SKU-VARIANT-1", 2, 5));

        assertThatThrownBy(() -> inventoryService.create(createRequest(reusedVariantId, "SKU-VARIANT-2", 2, 5)))
                .isInstanceOf(DuplicateInventoryItemException.class);
    }

    @Test
    void increasesAndDecreasesStock() {
        InventoryItemResponse item = createItem("SKU-ADJUST-1", 10);

        InventoryItemResponse increased = inventoryService.adjustStock(adjustRequest(item.id(), StockMovementType.INCREASE, 5));
        InventoryItemResponse decreased = inventoryService.adjustStock(adjustRequest(item.id(), StockMovementType.DECREASE, 3));

        assertThat(increased.quantityOnHand()).isEqualTo(15);
        assertThat(decreased.quantityOnHand()).isEqualTo(12);
        assertThat(decreased.availableQuantity()).isEqualTo(12);
    }

    @Test
    void rejectsInsufficientStock() {
        InventoryItemResponse item = createItem("SKU-LIMIT-1", 4);

        assertThatThrownBy(() -> inventoryService.reserve(reserveRequest(item.id(), 5, "ORDER-LIMIT-1", "RES-LIMIT-1")))
                .isInstanceOf(InvalidStockStateException.class);

        assertThatThrownBy(() -> inventoryService.adjustStock(adjustRequest(item.id(), StockMovementType.DECREASE, 5)))
                .isInstanceOf(InvalidStockStateException.class);
    }

    @Test
    void createsIdempotentReservationForSameReference() {
        InventoryItemResponse item = createItem("SKU-RESERVE-1", 10);

        StockReservationResponse first = inventoryService.reserve(reserveRequest(item.id(), 6, "ORDER-1", "RES-1"));
        StockReservationResponse second = inventoryService.reserve(reserveRequest(item.id(), 6, "ORDER-1", "RES-1"));

        InventoryItemResponse inventory = inventoryService.getById(item.id());
        assertThat(second.id()).isEqualTo(first.id());
        assertThat(second.status()).isEqualTo(StockReservationStatus.ACTIVE);
        assertThat(inventory.reservedQuantity()).isEqualTo(6);
        assertThat(inventory.availableQuantity()).isEqualTo(4);
    }

    @Test
    void rejectsDuplicateReservationKeyWithDifferentQuantity() {
        InventoryItemResponse item = createItem("SKU-RESERVE-2", 10);

        inventoryService.reserve(reserveRequest(item.id(), 4, "ORDER-2", "RES-2"));

        assertThatThrownBy(() -> inventoryService.reserve(reserveRequest(item.id(), 5, "ORDER-2", "RES-2")))
                .isInstanceOf(InvalidReservationStateException.class);
    }

    @Test
    void releasesReservation() {
        InventoryItemResponse item = createItem("SKU-RELEASE-1", 10);
        StockReservationResponse reservation = inventoryService.reserve(reserveRequest(item.id(), 6, "ORDER-3", "RES-3"));

        StockReservationResponse released = inventoryService.release(releaseRequest(reservation.id(), 6));
        InventoryItemResponse inventory = inventoryService.getById(item.id());

        assertThat(released.status()).isEqualTo(StockReservationStatus.RELEASED);
        assertThat(inventory.reservedQuantity()).isZero();
        assertThat(inventory.availableQuantity()).isEqualTo(10);
    }

    @Test
    void consumesReservation() {
        InventoryItemResponse item = createItem("SKU-CONSUME-1", 10);
        StockReservationResponse reservation = inventoryService.reserve(reserveRequest(item.id(), 4, "ORDER-4", "RES-4"));

        StockReservationResponse consumed = inventoryService.consume(consumeRequest(reservation.id(), 4));
        InventoryItemResponse inventory = inventoryService.getById(item.id());

        assertThat(consumed.status()).isEqualTo(StockReservationStatus.CONSUMED);
        assertThat(inventory.quantityOnHand()).isEqualTo(6);
        assertThat(inventory.reservedQuantity()).isZero();
        assertThat(inventory.availableQuantity()).isEqualTo(6);
    }

    @Test
    void rejectsOperationsOnClosedReservation() {
        InventoryItemResponse item = createItem("SKU-CLOSED-1", 10);
        StockReservationResponse reservation = inventoryService.reserve(reserveRequest(item.id(), 2, "ORDER-5", "RES-5"));
        inventoryService.release(releaseRequest(reservation.id(), 2));

        assertThatThrownBy(() -> inventoryService.consume(consumeRequest(reservation.id(), 1)))
                .isInstanceOf(InvalidReservationStateException.class);
        assertThatThrownBy(() -> inventoryService.release(releaseRequest(reservation.id(), 1)))
                .isInstanceOf(InvalidReservationStateException.class);
    }

    @Test
    void listsLowStockAndBuildsAvailabilityResponse() {
        InventoryItemResponse item = createItem("SKU-LOW-1", 4, 5);

        AvailabilityResponse availability = inventoryService.validateStock(new ValidateStockRequest(item.productVariantId(), null, 4));

        assertThat(availability.available()).isTrue();
        assertThat(availability.lowStock()).isTrue();
        assertThat(inventoryService.listLowStock(5, PageRequest.of(0, 10)).getContent())
                .extracting(InventoryItemResponse::id)
                .contains(item.id());
    }

    @Test
    void deactivatedInventoryCannotBeReserved() {
        InventoryItemResponse item = createItem("SKU-INACTIVE-1", 5);
        inventoryService.deactivate(item.id());

        assertThatThrownBy(() -> inventoryService.reserve(reserveRequest(item.id(), 1, "ORDER-6", "RES-6")))
                .isInstanceOf(InvalidStockStateException.class);
    }

    @Test
    void recordsMovementsWithReservationFlow() {
        InventoryItemResponse item = createItem("SKU-MOVE-1", 5);

        inventoryService.adjustStock(adjustRequest(item.id(), StockMovementType.INCREASE, 2));
        StockReservationResponse reservation = inventoryService.reserve(reserveRequest(item.id(), 3, "ORDER-7", "RES-7"));
        inventoryService.release(releaseRequest(reservation.id(), 3));

        assertThat(inventoryService.listMovements(item.id(), null, PageRequest.of(0, 10)).getTotalElements())
                .isEqualTo(4);
    }

    @Test
    void returnsInventorySummary() {
        createItem("SKU-SUMMARY-1", 3, 5);
        createItem("SKU-SUMMARY-2", 20, 5);

        assertThat(inventoryService.summary().itemCount()).isGreaterThanOrEqualTo(2);
        assertThat(inventoryService.summary().lowStockItemCount()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void externalCatalogFailureBecomesServiceUnavailable() {
        UUID variantId = UUID.randomUUID();
        when(catalogVariantClient.getVariant(variantId)).thenThrow(new RuntimeException("catalog down"));

        assertThatThrownBy(() -> inventoryService.create(createRequest(variantId, "SKU-DOWN-1", 1, 5)))
                .isInstanceOf(ExternalServiceUnavailableException.class);
    }

    private InventoryItemResponse createItem(String sku, int quantityOnHand) {
        return createItem(sku, quantityOnHand, 5);
    }

    private InventoryItemResponse createItem(String sku, int quantityOnHand, int lowStockThreshold) {
        UUID variantId = UUID.randomUUID();
        mockCatalogVariant(variantId, sku);
        return inventoryService.create(createRequest(variantId, sku, quantityOnHand, lowStockThreshold));
    }

    private CreateInventoryItemRequest createRequest(UUID variantId, String sku, int quantityOnHand,
                                                     int lowStockThreshold) {
        return new CreateInventoryItemRequest(variantId, sku, quantityOnHand, 0, true, lowStockThreshold);
    }

    private AdjustStockRequest adjustRequest(UUID inventoryItemId, StockMovementType movementType, int quantity) {
        return new AdjustStockRequest(
                inventoryItemId,
                null,
                null,
                movementType,
                quantity,
                "test adjustment",
                ReferenceType.ADMIN,
                "admin-test"
        );
    }

    private ReserveStockRequest reserveRequest(UUID inventoryItemId, int quantity, String referenceId,
                                               String reservationKey) {
        return new ReserveStockRequest(
                inventoryItemId,
                null,
                null,
                quantity,
                "test reserve",
                ReferenceType.ORDER,
                referenceId,
                reservationKey,
                null
        );
    }

    private ReleaseStockRequest releaseRequest(UUID reservationId, int quantity) {
        return new ReleaseStockRequest(
                reservationId,
                null,
                null,
                null,
                null,
                quantity,
                "test release",
                ReferenceType.ORDER,
                null
        );
    }

    private ConsumeStockReservationRequest consumeRequest(UUID reservationId, int quantity) {
        return new ConsumeStockReservationRequest(reservationId, null, quantity, "test consume");
    }

    private void mockCatalogVariant(UUID variantId, String sku) {
        when(catalogVariantClient.getVariant(variantId)).thenReturn(catalogVariant(variantId, sku));
    }

    private CatalogVariantResponse catalogVariant(UUID variantId, String sku) {
        return new CatalogVariantResponse(
                variantId,
                UUID.randomUUID(),
                "Test Product",
                "test-product",
                "ACTIVE",
                sku,
                "Black",
                "M",
                true
        );
    }
}
