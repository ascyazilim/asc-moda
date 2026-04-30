package com.ascmoda.cart.application.service;

import com.ascmoda.cart.api.dto.AddCartItemRequest;
import com.ascmoda.cart.api.dto.CartRefreshResponse;
import com.ascmoda.cart.api.dto.CartResponse;
import com.ascmoda.cart.api.dto.CartValidationIssueType;
import com.ascmoda.cart.api.dto.CartValidationResponse;
import com.ascmoda.cart.api.dto.CheckoutPreviewResponse;
import com.ascmoda.cart.api.dto.ToggleCartItemSelectionRequest;
import com.ascmoda.cart.api.dto.UpdateCartItemQuantityRequest;
import com.ascmoda.cart.api.error.CartNotFoundException;
import com.ascmoda.cart.api.error.ExternalCatalogException;
import com.ascmoda.cart.api.error.ExternalServiceUnavailableException;
import com.ascmoda.cart.api.error.InsufficientStockException;
import com.ascmoda.cart.domain.model.CartStatus;
import com.ascmoda.cart.infrastructure.catalog.CatalogVariantClient;
import com.ascmoda.cart.infrastructure.catalog.CatalogVariantDetailResponse;
import com.ascmoda.cart.infrastructure.inventory.InventoryClient;
import com.ascmoda.cart.infrastructure.inventory.InventoryItemResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
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
        "ascmoda.cart.config-source=test"
})
@Testcontainers(disabledWithoutDocker = true)
@Transactional
class CartServiceIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private CartService cartService;

    @MockitoBean
    private CatalogVariantClient catalogVariantClient;

    @MockitoBean
    private InventoryClient inventoryClient;

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> "true");
    }

    @Test
    void createsOrReturnsActiveCartForCustomer() {
        UUID customerId = UUID.randomUUID();

        CartResponse first = cartService.getOrCreateActiveCart(customerId);
        CartResponse second = cartService.getOrCreateActiveCart(customerId);

        assertThat(first.id()).isEqualTo(second.id());
        assertThat(second.customerId()).isEqualTo(customerId);
        assertThat(second.status()).isEqualTo(CartStatus.ACTIVE);
        assertThat(second.items()).isEmpty();
    }

    @Test
    void addsItemToCartWithSnapshots() {
        UUID customerId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();
        mockCatalogAndInventory(variantId, "SKU-CART-1", 10);

        CartResponse cart = cartService.addItem(customerId, addRequest(variantId, "SKU-CART-1", 2));

        assertThat(cart.items()).hasSize(1);
        assertThat(cart.items().get(0).productVariantId()).isEqualTo(variantId);
        assertThat(cart.items().get(0).productNameSnapshot()).isEqualTo("Cotton Shirt");
        assertThat(cart.items().get(0).productSlugSnapshot()).isEqualTo("cotton-shirt");
        assertThat(cart.items().get(0).mainImageUrlSnapshot()).isEqualTo("https://assets.ascmoda.test/cotton-shirt.jpg");
        assertThat(cart.items().get(0).lineTotal()).isEqualByComparingTo("39.98");
    }

    @Test
    void addingSameVariantIncreasesQuantity() {
        UUID customerId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();
        mockCatalogAndInventory(variantId, "SKU-CART-2", 10);

        cartService.addItem(customerId, addRequest(variantId, "SKU-CART-2", 2));
        CartResponse cart = cartService.addItem(customerId, addRequest(variantId, "SKU-CART-2", 3));

        assertThat(cart.items()).hasSize(1);
        assertThat(cart.items().get(0).quantity()).isEqualTo(5);
    }

    @Test
    void rejectsInactiveProduct() {
        UUID customerId = UUID.randomUUID();
        UUID inactiveProductVariantId = UUID.randomUUID();

        when(catalogVariantClient.getVariantDetails(inactiveProductVariantId))
                .thenReturn(catalogVariant(inactiveProductVariantId, "SKU-INACTIVE-PRODUCT", "INACTIVE", true));

        assertThatThrownBy(() -> cartService.addItem(
                customerId,
                addRequest(inactiveProductVariantId, "SKU-INACTIVE-PRODUCT", 1)
        )).isInstanceOf(ExternalCatalogException.class);
    }

    @Test
    void rejectsInactiveVariant() {
        UUID customerId = UUID.randomUUID();
        UUID inactiveVariantId = UUID.randomUUID();

        when(catalogVariantClient.getVariantDetails(inactiveVariantId))
                .thenReturn(catalogVariant(inactiveVariantId, "SKU-INACTIVE-VARIANT", "ACTIVE", false));

        assertThatThrownBy(() -> cartService.addItem(
                customerId,
                addRequest(inactiveVariantId, "SKU-INACTIVE-VARIANT", 1)
        )).isInstanceOf(ExternalCatalogException.class);
    }

    @Test
    void rejectsInsufficientStockWhenAdding() {
        UUID customerId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();
        mockCatalogAndInventory(variantId, "SKU-CART-3", 2);

        assertThatThrownBy(() -> cartService.addItem(customerId, addRequest(variantId, "SKU-CART-3", 3)))
                .isInstanceOf(InsufficientStockException.class);
    }

    @Test
    void rejectsInsufficientStockWhenUpdatingQuantity() {
        UUID customerId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();
        mockCatalogAndInventory(variantId, "SKU-CART-3B", 5);
        CartResponse cart = cartService.addItem(customerId, addRequest(variantId, "SKU-CART-3B", 2));
        mockInventory(variantId, "SKU-CART-3B", 2);

        assertThatThrownBy(() -> cartService.updateItemQuantity(
                customerId,
                cart.items().get(0).id(),
                new UpdateCartItemQuantityRequest(3)
        )).isInstanceOf(InsufficientStockException.class);
    }

    @Test
    void updatesItemQuantityAndSelection() {
        UUID customerId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();
        mockCatalogAndInventory(variantId, "SKU-CART-4", 10);

        CartResponse cart = cartService.addItem(customerId, addRequest(variantId, "SKU-CART-4", 2));
        UUID itemId = cart.items().get(0).id();
        CartResponse updated = cartService.updateItemQuantity(customerId, itemId, new UpdateCartItemQuantityRequest(7));
        CartResponse unselected = cartService.toggleItemSelection(
                customerId,
                itemId,
                new ToggleCartItemSelectionRequest(false)
        );

        assertThat(updated.items().get(0).quantity()).isEqualTo(7);
        assertThat(updated.selectedTotal()).isEqualByComparingTo("139.93");
        assertThat(unselected.items().get(0).selected()).isFalse();
        assertThat(unselected.selectedTotal()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void removesItemAndClearsCart() {
        UUID customerId = UUID.randomUUID();
        UUID firstVariantId = UUID.randomUUID();
        UUID secondVariantId = UUID.randomUUID();
        mockCatalogAndInventory(firstVariantId, "SKU-CART-5", 10);
        mockCatalogAndInventory(secondVariantId, "SKU-CART-6", 10);

        CartResponse cart = cartService.addItem(customerId, addRequest(firstVariantId, "SKU-CART-5", 1));
        cartService.addItem(customerId, addRequest(secondVariantId, "SKU-CART-6", 1));

        CartResponse afterRemove = cartService.removeItem(customerId, cart.items().get(0).id());
        CartResponse afterClear = cartService.clearCart(customerId);

        assertThat(afterRemove.items()).hasSize(1);
        assertThat(afterClear.items()).isEmpty();
    }

    @Test
    void producesValidationResultWithItemIssues() {
        UUID customerId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();
        mockCatalogAndInventory(variantId, "SKU-CART-7", 10);
        cartService.addItem(customerId, addRequest(variantId, "SKU-CART-7", 2));

        mockInventory(variantId, "SKU-CART-7", 1);
        CartValidationResponse validation = cartService.validateActiveCart(customerId);

        assertThat(validation.valid()).isFalse();
        assertThat(validation.issues()).hasSize(1);
        assertThat(validation.issues().get(0).type()).isEqualTo(CartValidationIssueType.INSUFFICIENT_STOCK);
    }

    @Test
    void refreshUpdatesSnapshotAndReportsChanges() {
        UUID customerId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();
        when(catalogVariantClient.getVariantDetails(variantId))
                .thenReturn(catalogVariant(
                        variantId,
                        "SKU-REFRESH",
                        "Cotton Shirt",
                        "cotton-shirt",
                        "https://assets.ascmoda.test/cotton-shirt.jpg",
                        BigDecimal.valueOf(1999, 2),
                        "ACTIVE",
                        true
                ))
                .thenReturn(catalogVariant(
                        variantId,
                        "SKU-REFRESH",
                        "Updated Shirt",
                        "updated-shirt",
                        "https://assets.ascmoda.test/updated-shirt.jpg",
                        BigDecimal.valueOf(2499, 2),
                        "ACTIVE",
                        true
                ));
        mockInventory(variantId, "SKU-REFRESH", 10);

        cartService.addItem(customerId, addRequest(variantId, "SKU-REFRESH", 1));
        CartRefreshResponse refreshed = cartService.refresh(customerId);

        assertThat(refreshed.cart().items().get(0).productNameSnapshot()).isEqualTo("Updated Shirt");
        assertThat(refreshed.cart().items().get(0).productSlugSnapshot()).isEqualTo("updated-shirt");
        assertThat(refreshed.cart().items().get(0).unitPriceSnapshot()).isEqualByComparingTo("24.99");
        assertThat(refreshed.validation().issues())
                .extracting("type")
                .contains(CartValidationIssueType.PRICE_CHANGED, CartValidationIssueType.SNAPSHOT_OUTDATED);
    }

    @Test
    void checkoutPreviewUsesOnlySelectedItems() {
        UUID customerId = UUID.randomUUID();
        UUID selectedVariantId = UUID.randomUUID();
        UUID unselectedVariantId = UUID.randomUUID();
        mockCatalogAndInventory(selectedVariantId, "SKU-CHECKOUT-1", 10);
        mockCatalogAndInventory(unselectedVariantId, "SKU-CHECKOUT-2", 10);

        CartResponse cart = cartService.addItem(customerId, addRequest(selectedVariantId, "SKU-CHECKOUT-1", 2));
        CartResponse withSecondItem = cartService.addItem(customerId, addRequest(unselectedVariantId, "SKU-CHECKOUT-2", 3));
        UUID unselectedItemId = withSecondItem.items()
                .stream()
                .filter(item -> item.productVariantId().equals(unselectedVariantId))
                .findFirst()
                .orElseThrow()
                .id();
        cartService.toggleItemSelection(customerId, unselectedItemId, new ToggleCartItemSelectionRequest(false));

        CheckoutPreviewResponse preview = cartService.getCheckoutPreview(customerId);

        assertThat(preview.selectedItems()).hasSize(1);
        assertThat(preview.selectedItems().get(0).productVariantId()).isEqualTo(selectedVariantId);
        assertThat(preview.selectedTotal()).isEqualByComparingTo("39.98");
        assertThat(cart.id()).isEqualTo(preview.cartId());
    }

    @Test
    void closedCartIsNotReturnedAsActiveForFurtherItemMutation() {
        UUID customerId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();
        mockCatalogAndInventory(variantId, "SKU-CLOSED", 10);
        CartResponse cart = cartService.addItem(customerId, addRequest(variantId, "SKU-CLOSED", 1));

        CartResponse checkedOut = cartService.markActiveCartCheckedOut(customerId);

        assertThat(checkedOut.status()).isEqualTo(CartStatus.CHECKED_OUT);
        assertThat(checkedOut.checkedOutAt()).isNotNull();
        assertThatThrownBy(() -> cartService.updateItemQuantity(
                customerId,
                cart.items().get(0).id(),
                new UpdateCartItemQuantityRequest(2)
        )).isInstanceOf(CartNotFoundException.class);
    }

    @Test
    void abandonedCartIsNotReturnedAsActiveForFurtherItemMutation() {
        UUID customerId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();
        mockCatalogAndInventory(variantId, "SKU-ABANDONED", 10);
        CartResponse cart = cartService.addItem(customerId, addRequest(variantId, "SKU-ABANDONED", 1));

        CartResponse abandoned = cartService.markActiveCartAbandoned(customerId);

        assertThat(abandoned.status()).isEqualTo(CartStatus.ABANDONED);
        assertThat(abandoned.abandonedAt()).isNotNull();
        assertThatThrownBy(() -> cartService.removeItem(customerId, cart.items().get(0).id()))
                .isInstanceOf(CartNotFoundException.class);
    }

    @Test
    void externalCatalogFailureBecomesDomainException() {
        UUID customerId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();
        when(catalogVariantClient.getVariantDetails(variantId)).thenThrow(new RuntimeException("catalog down"));

        assertThatThrownBy(() -> cartService.addItem(customerId, addRequest(variantId, "SKU-DOWN", 1)))
                .isInstanceOf(ExternalServiceUnavailableException.class);
    }

    private AddCartItemRequest addRequest(UUID variantId, String sku, int quantity) {
        return new AddCartItemRequest(variantId, sku, quantity);
    }

    private void mockCatalogAndInventory(UUID variantId, String sku, int availableQuantity) {
        when(catalogVariantClient.getVariantDetails(variantId)).thenReturn(catalogVariant(variantId, sku));
        mockInventory(variantId, sku, availableQuantity);
    }

    private CatalogVariantDetailResponse catalogVariant(UUID variantId, String sku) {
        return catalogVariant(variantId, sku, "ACTIVE", true);
    }

    private CatalogVariantDetailResponse catalogVariant(UUID variantId, String sku, String productStatus,
                                                       boolean variantActive) {
        return catalogVariant(
                variantId,
                sku,
                "Cotton Shirt",
                "cotton-shirt",
                "https://assets.ascmoda.test/cotton-shirt.jpg",
                BigDecimal.valueOf(1999, 2),
                productStatus,
                variantActive
        );
    }

    private CatalogVariantDetailResponse catalogVariant(UUID variantId, String sku, String productName,
                                                       String productSlug, String mainImageUrl,
                                                       BigDecimal effectiveUnitPrice, String productStatus,
                                                       boolean variantActive) {
        return new CatalogVariantDetailResponse(
                variantId,
                UUID.randomUUID(),
                productName,
                productSlug,
                productStatus,
                sku,
                "Black",
                "M",
                effectiveUnitPrice,
                mainImageUrl,
                variantActive
        );
    }

    private void mockInventory(UUID variantId, String sku, int availableQuantity) {
        when(inventoryClient.getByProductVariantId(variantId))
                .thenReturn(new InventoryItemResponse(
                        UUID.randomUUID(),
                        variantId,
                        sku,
                        availableQuantity,
                        0,
                        availableQuantity,
                        true
                ));
    }
}
