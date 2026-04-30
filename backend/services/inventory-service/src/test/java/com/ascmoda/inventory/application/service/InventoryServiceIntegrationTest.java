package com.ascmoda.inventory.application.service;

import com.ascmoda.inventory.api.dto.AdjustStockRequest;
import com.ascmoda.inventory.api.dto.CreateInventoryItemRequest;
import com.ascmoda.inventory.api.dto.InventoryItemResponse;
import com.ascmoda.inventory.api.dto.ReleaseStockRequest;
import com.ascmoda.inventory.api.dto.ReserveStockRequest;
import com.ascmoda.inventory.api.error.DuplicateInventoryItemException;
import com.ascmoda.inventory.api.error.InvalidStockStateException;
import com.ascmoda.inventory.domain.model.ReferenceType;
import com.ascmoda.inventory.domain.model.StockMovementType;
import com.ascmoda.inventory.infrastructure.catalog.CatalogVariantClient;
import com.ascmoda.inventory.infrastructure.catalog.CatalogVariantResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
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
    void createsInventoryItem() {
        UUID variantId = UUID.randomUUID();
        mockCatalogVariant(variantId, "SKU-CREATE-1");

        InventoryItemResponse item = inventoryService.create(createRequest(variantId, "SKU-CREATE-1", 12));

        assertThat(item.productVariantId()).isEqualTo(variantId);
        assertThat(item.sku()).isEqualTo("SKU-CREATE-1");
        assertThat(item.quantityOnHand()).isEqualTo(12);
        assertThat(item.availableQuantity()).isEqualTo(12);
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
    void reservesAndReleasesStock() {
        InventoryItemResponse item = createItem("SKU-RESERVE-1", 10);

        InventoryItemResponse reserved = inventoryService.reserve(reserveRequest(item.id(), 6));
        InventoryItemResponse released = inventoryService.release(releaseRequest(item.id(), 4));

        assertThat(reserved.reservedQuantity()).isEqualTo(6);
        assertThat(reserved.availableQuantity()).isEqualTo(4);
        assertThat(released.reservedQuantity()).isEqualTo(2);
        assertThat(released.availableQuantity()).isEqualTo(8);
    }

    @Test
    void rejectsInsufficientStockAndInvalidRelease() {
        InventoryItemResponse item = createItem("SKU-LIMIT-1", 4);

        assertThatThrownBy(() -> inventoryService.reserve(reserveRequest(item.id(), 5)))
                .isInstanceOf(InvalidStockStateException.class);

        inventoryService.reserve(reserveRequest(item.id(), 2));

        assertThatThrownBy(() -> inventoryService.release(releaseRequest(item.id(), 3)))
                .isInstanceOf(InvalidStockStateException.class);
    }

    @Test
    void rejectsDuplicateSku() {
        InventoryItemResponse item = createItem("SKU-DUP-1", 3);
        UUID secondVariantId = UUID.randomUUID();
        mockCatalogVariant(secondVariantId, item.sku());

        assertThatThrownBy(() -> inventoryService.create(createRequest(secondVariantId, item.sku(), 2)))
                .isInstanceOf(DuplicateInventoryItemException.class);
    }

    @Test
    void recordsStockMovements() {
        InventoryItemResponse item = createItem("SKU-MOVE-1", 5);

        inventoryService.adjustStock(adjustRequest(item.id(), StockMovementType.INCREASE, 2));
        inventoryService.reserve(reserveRequest(item.id(), 3));
        inventoryService.release(releaseRequest(item.id(), 1));

        assertThat(inventoryService.listMovements(item.id(), null, PageRequest.of(0, 10)).getTotalElements())
                .isEqualTo(4);
    }

    private InventoryItemResponse createItem(String sku, int quantityOnHand) {
        UUID variantId = UUID.randomUUID();
        mockCatalogVariant(variantId, sku);
        return inventoryService.create(createRequest(variantId, sku, quantityOnHand));
    }

    private CreateInventoryItemRequest createRequest(UUID variantId, String sku, int quantityOnHand) {
        return new CreateInventoryItemRequest(variantId, sku, quantityOnHand, 0, true);
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
                null
        );
    }

    private ReserveStockRequest reserveRequest(UUID inventoryItemId, int quantity) {
        return new ReserveStockRequest(inventoryItemId, null, null, quantity, "test reserve", ReferenceType.ORDER, null);
    }

    private ReleaseStockRequest releaseRequest(UUID inventoryItemId, int quantity) {
        return new ReleaseStockRequest(inventoryItemId, null, null, quantity, "test release", ReferenceType.ORDER, null);
    }

    private void mockCatalogVariant(UUID variantId, String sku) {
        when(catalogVariantClient.getVariant(variantId))
                .thenReturn(new CatalogVariantResponse(variantId, sku, true));
    }
}
