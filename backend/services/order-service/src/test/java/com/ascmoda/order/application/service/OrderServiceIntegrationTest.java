package com.ascmoda.order.application.service;

import com.ascmoda.order.controller.dto.AddressSnapshotRequest;
import com.ascmoda.order.controller.dto.CancelOrderRequest;
import com.ascmoda.order.controller.dto.CreateOrderRequest;
import com.ascmoda.order.controller.dto.OrderListItemResponse;
import com.ascmoda.order.controller.dto.OrderResponse;
import com.ascmoda.order.controller.dto.OrderSummaryResponse;
import com.ascmoda.order.controller.dto.PageResponse;
import com.ascmoda.order.domain.exception.CheckoutPreviewInvalidException;
import com.ascmoda.order.domain.exception.EmptyCheckoutSelectionException;
import com.ascmoda.order.domain.exception.ExternalServiceUnavailableException;
import com.ascmoda.order.domain.exception.InventoryConsumeFailedException;
import com.ascmoda.order.domain.exception.InventoryReleaseFailedException;
import com.ascmoda.order.domain.exception.InventoryReservationFailedException;
import com.ascmoda.order.domain.exception.InvalidOrderStateException;
import com.ascmoda.order.domain.model.OrderReservationStatus;
import com.ascmoda.order.domain.model.OrderSource;
import com.ascmoda.order.domain.model.OrderStatus;
import com.ascmoda.order.domain.repository.OrderRepository;
import com.ascmoda.order.infrastructure.cart.CartClient;
import com.ascmoda.order.infrastructure.cart.CartItemResponse;
import com.ascmoda.order.infrastructure.cart.CartResponse;
import com.ascmoda.order.infrastructure.cart.CartValidationIssueResponse;
import com.ascmoda.order.infrastructure.cart.CartValidationResponse;
import com.ascmoda.order.infrastructure.cart.CheckoutPreviewResponse;
import com.ascmoda.order.infrastructure.inventory.ConsumeStockReservationRequest;
import com.ascmoda.order.infrastructure.inventory.InventoryClient;
import com.ascmoda.order.infrastructure.inventory.ReferenceType;
import com.ascmoda.order.infrastructure.inventory.ReleaseStockRequest;
import com.ascmoda.order.infrastructure.inventory.ReserveStockRequest;
import com.ascmoda.order.infrastructure.inventory.StockReservationResponse;
import com.ascmoda.order.infrastructure.inventory.StockReservationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
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
        "ascmoda.order.config-source=test",
        "ascmoda.order.reservation-ttl-minutes=30"
})
@Testcontainers(disabledWithoutDocker = true)
class OrderServiceIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @MockitoBean
    private CartClient cartClient;

    @MockitoBean
    private InventoryClient inventoryClient;

    @MockitoBean
    private OrderNumberGenerator orderNumberGenerator;

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> "true");
    }

    @BeforeEach
    void cleanDatabase() {
        orderRepository.deleteAll();
        reset(cartClient, inventoryClient, orderNumberGenerator);
        when(orderNumberGenerator.generate()).thenAnswer(invocation ->
                "ORD-TEST-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        when(inventoryClient.reserve(any(ReserveStockRequest.class))).thenAnswer(invocation -> {
            ReserveStockRequest request = invocation.getArgument(0);
            return reservationResponse(request.productVariantId(), request.sku(), request.quantity(), request.reservationKey());
        });
        when(cartClient.markCheckedOut(any(UUID.class))).thenAnswer(invocation ->
                new CartResponse(UUID.randomUUID(), invocation.getArgument(0), "CHECKED_OUT", Instant.now()));
    }

    @Test
    void createsOrderWhenCheckoutPreviewIsValid() {
        UUID customerId = UUID.randomUUID();
        UUID cartId = UUID.randomUUID();
        CartItemResponse item = cartItem("SKU-ORDER-1", 2, BigDecimal.valueOf(1999, 2));
        when(cartClient.getCheckoutPreview(customerId)).thenReturn(validPreview(cartId, customerId, List.of(item)));

        OrderResponse order = orderService.createOrder(createRequest(customerId));

        assertThat(order.status()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.customerId()).isEqualTo(customerId);
        assertThat(order.sourceCartId()).isEqualTo(cartId);
        assertThat(order.currency()).isEqualTo("TRY");
        assertThat(order.idempotencyKey()).isEqualTo("checkout-" + customerId);
        assertThat(order.externalReference()).isEqualTo("web-" + customerId);
        assertThat(order.paymentReference()).isNull();
        assertThat(order.totalAmount()).isEqualByComparingTo("39.98");
        assertThat(order.items()).hasSize(1);
        assertThat(order.items().get(0).productNameSnapshot()).isEqualTo("Cotton Shirt");
        assertThat(order.items().get(0).lineTotal()).isEqualByComparingTo("39.98");
        assertThat(order.items().get(0).reservationStatus()).isEqualTo(OrderReservationStatus.ACTIVE);
        verify(inventoryClient).reserve(any(ReserveStockRequest.class));
        verify(cartClient).markCheckedOut(customerId);
    }

    @Test
    void rejectsInvalidCheckoutPreviewWithoutReservation() {
        UUID customerId = UUID.randomUUID();
        UUID cartId = UUID.randomUUID();
        CartItemResponse item = cartItem("SKU-INVALID", 1, BigDecimal.TEN);
        when(cartClient.getCheckoutPreview(customerId)).thenReturn(invalidPreview(cartId, customerId, item));

        assertThatThrownBy(() -> orderService.createOrder(createRequest(customerId)))
                .isInstanceOf(CheckoutPreviewInvalidException.class);

        assertThat(orderRepository.count()).isZero();
        verify(inventoryClient, never()).reserve(any(ReserveStockRequest.class));
        verify(cartClient, never()).markCheckedOut(any(UUID.class));
    }

    @Test
    void rejectsEmptyCheckoutSelectionWithoutReservation() {
        UUID customerId = UUID.randomUUID();
        UUID cartId = UUID.randomUUID();
        when(cartClient.getCheckoutPreview(customerId)).thenReturn(validPreview(cartId, customerId, List.of()));

        assertThatThrownBy(() -> orderService.createOrder(createRequest(customerId)))
                .isInstanceOf(EmptyCheckoutSelectionException.class);

        assertThat(orderRepository.count()).isZero();
        verify(inventoryClient, never()).reserve(any(ReserveStockRequest.class));
        verify(cartClient, never()).markCheckedOut(any(UUID.class));
    }

    @Test
    void rejectsInventoryReservationFailureWithoutCreatingOrder() {
        UUID customerId = UUID.randomUUID();
        UUID cartId = UUID.randomUUID();
        CartItemResponse item = cartItem("SKU-RESERVE-FAIL", 1, BigDecimal.TEN);
        when(cartClient.getCheckoutPreview(customerId)).thenReturn(validPreview(cartId, customerId, List.of(item)));
        when(inventoryClient.reserve(any(ReserveStockRequest.class)))
                .thenThrow(new InventoryReservationFailedException("reserve failed"));

        assertThatThrownBy(() -> orderService.createOrder(createRequest(customerId)))
                .isInstanceOf(InventoryReservationFailedException.class);

        assertThat(orderRepository.count()).isZero();
        verify(cartClient, never()).markCheckedOut(any(UUID.class));
    }

    @Test
    void releasesReservationWhenCartCheckoutMarkFails() {
        UUID customerId = UUID.randomUUID();
        UUID cartId = UUID.randomUUID();
        CartItemResponse item = cartItem("SKU-COMPENSATE", 1, BigDecimal.TEN);
        when(cartClient.getCheckoutPreview(customerId)).thenReturn(validPreview(cartId, customerId, List.of(item)));
        when(cartClient.markCheckedOut(customerId)).thenThrow(new RuntimeException("cart down"));

        assertThatThrownBy(() -> orderService.createOrder(createRequest(customerId)))
                .isInstanceOf(ExternalServiceUnavailableException.class);

        assertThat(orderRepository.count()).isZero();
        verify(inventoryClient).release(any(ReleaseStockRequest.class));
    }

    @Test
    void releasesReservationWhenPersistFailsAfterReserve() {
        UUID customerId = UUID.randomUUID();
        UUID cartId = UUID.randomUUID();
        CartItemResponse item = cartItem(
                "SKU-PERSIST-FAIL",
                1,
                BigDecimal.TEN,
                "X".repeat(221)
        );
        when(cartClient.getCheckoutPreview(customerId)).thenReturn(validPreview(cartId, customerId, List.of(item)));

        assertThatThrownBy(() -> orderService.createOrder(createRequest(customerId)))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThat(orderRepository.count()).isZero();
        verify(inventoryClient).release(any(ReleaseStockRequest.class));
        verify(cartClient, never()).markCheckedOut(any(UUID.class));
    }

    @Test
    void duplicateCreateWithIdempotencyKeyReturnsExistingOrderWithoutExternalCalls() {
        UUID customerId = UUID.randomUUID();
        UUID cartId = UUID.randomUUID();
        CartItemResponse item = cartItem("SKU-IDEMPOTENT", 1, BigDecimal.TEN);
        when(cartClient.getCheckoutPreview(customerId)).thenReturn(validPreview(cartId, customerId, List.of(item)));

        OrderResponse first = orderService.createOrder(createRequest(customerId));
        clearInvocations(cartClient, inventoryClient);

        OrderResponse second = orderService.createOrder(createRequest(customerId));

        assertThat(second.id()).isEqualTo(first.id());
        assertThat(second.orderNumber()).isEqualTo(first.orderNumber());
        verify(cartClient, never()).getCheckoutPreview(any(UUID.class));
        verify(inventoryClient, never()).reserve(any(ReserveStockRequest.class));
        verify(cartClient, never()).markCheckedOut(any(UUID.class));
    }

    @Test
    void duplicateCreateForSourceCartReturnsExistingOrderWithoutSecondReservation() {
        UUID customerId = UUID.randomUUID();
        UUID cartId = UUID.randomUUID();
        CartItemResponse item = cartItem("SKU-CART-IDEMPOTENT", 1, BigDecimal.TEN);
        when(cartClient.getCheckoutPreview(customerId)).thenReturn(validPreview(cartId, customerId, List.of(item)));

        OrderResponse first = orderService.createOrder(createRequest(customerId, null, null));
        clearInvocations(inventoryClient, cartClient);

        OrderResponse second = orderService.createOrder(createRequest(customerId, null, null));

        assertThat(second.id()).isEqualTo(first.id());
        verify(cartClient).getCheckoutPreview(customerId);
        verify(inventoryClient, never()).reserve(any(ReserveStockRequest.class));
        verify(cartClient, never()).markCheckedOut(any(UUID.class));
    }

    @Test
    void confirmsOrderAndConsumesReservation() {
        OrderResponse created = createPersistedOrder("SKU-CONFIRM", UUID.randomUUID());
        clearInvocations(inventoryClient);

        OrderResponse confirmed = orderService.confirm(created.id());

        assertThat(confirmed.status()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(confirmed.confirmedAt()).isNotNull();
        assertThat(confirmed.items().get(0).reservationStatus()).isEqualTo(OrderReservationStatus.CONSUMED);
        ArgumentCaptor<ConsumeStockReservationRequest> captor = ArgumentCaptor.forClass(ConsumeStockReservationRequest.class);
        verify(inventoryClient).consume(captor.capture());
        assertThat(captor.getValue().reservationKey()).isEqualTo(created.items().get(0).reservationKey());
    }

    @Test
    void consumeFailurePreventsConfirmationWithoutDuplicateStateMutation() {
        OrderResponse created = createPersistedOrder("SKU-CONSUME-FAIL", UUID.randomUUID());
        clearInvocations(inventoryClient);
        when(inventoryClient.consume(any(ConsumeStockReservationRequest.class)))
                .thenThrow(new InventoryConsumeFailedException("consume failed"));

        assertThatThrownBy(() -> orderService.confirm(created.id()))
                .isInstanceOf(InventoryConsumeFailedException.class);

        OrderResponse current = orderService.getOrder(created.id());
        assertThat(current.status()).isEqualTo(OrderStatus.PENDING);
        assertThat(current.items().get(0).reservationStatus()).isEqualTo(OrderReservationStatus.ACTIVE);
    }

    @Test
    void rejectsDuplicateConfirm() {
        OrderResponse created = createPersistedOrder("SKU-CONFIRM-ONCE", UUID.randomUUID());
        orderService.confirm(created.id());
        clearInvocations(inventoryClient);

        assertThatThrownBy(() -> orderService.confirm(created.id()))
                .isInstanceOf(InvalidOrderStateException.class);

        verify(inventoryClient, never()).consume(any(ConsumeStockReservationRequest.class));
    }

    @Test
    void cancelsOrderAndReleasesReservation() {
        OrderResponse created = createPersistedOrder("SKU-CANCEL", UUID.randomUUID());
        clearInvocations(inventoryClient);

        OrderResponse cancelled = orderService.cancel(created.id(), new CancelOrderRequest("Customer requested cancellation"));

        assertThat(cancelled.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(cancelled.cancelledAt()).isNotNull();
        assertThat(cancelled.cancellationReason()).isEqualTo("Customer requested cancellation");
        assertThat(cancelled.items().get(0).reservationStatus()).isEqualTo(OrderReservationStatus.RELEASED);
        verify(inventoryClient).release(any(ReleaseStockRequest.class));
    }

    @Test
    void releaseFailurePreventsCancellationWithoutMarkingReservationReleased() {
        OrderResponse created = createPersistedOrder("SKU-RELEASE-FAIL", UUID.randomUUID());
        clearInvocations(inventoryClient);
        when(inventoryClient.release(any(ReleaseStockRequest.class)))
                .thenThrow(new InventoryReleaseFailedException("release failed"));

        assertThatThrownBy(() -> orderService.cancel(created.id(), new CancelOrderRequest("Customer requested cancellation")))
                .isInstanceOf(InventoryReleaseFailedException.class);

        OrderResponse current = orderService.getOrder(created.id());
        assertThat(current.status()).isEqualTo(OrderStatus.PENDING);
        assertThat(current.items().get(0).reservationStatus()).isEqualTo(OrderReservationStatus.ACTIVE);
    }

    @Test
    void rejectsInvalidStateTransitionsAfterCancellation() {
        OrderResponse created = createPersistedOrder("SKU-CANCEL-ONCE", UUID.randomUUID());
        orderService.cancel(created.id(), new CancelOrderRequest("No longer needed"));
        clearInvocations(inventoryClient);

        assertThatThrownBy(() -> orderService.cancel(created.id(), new CancelOrderRequest("Again")))
                .isInstanceOf(InvalidOrderStateException.class);
        assertThatThrownBy(() -> orderService.confirm(created.id()))
                .isInstanceOf(InvalidOrderStateException.class);

        verify(inventoryClient, never()).release(any(ReleaseStockRequest.class));
        verify(inventoryClient, never()).consume(any(ConsumeStockReservationRequest.class));
    }

    @Test
    void listsCustomerOrdersAndAdminFilters() {
        UUID firstCustomer = UUID.randomUUID();
        UUID secondCustomer = UUID.randomUUID();
        OrderResponse first = createPersistedOrder("SKU-LIST-1", firstCustomer);
        createPersistedOrder("SKU-LIST-2", secondCustomer);

        PageResponse<OrderSummaryResponse> customerOrders = orderService.listCustomerOrders(firstCustomer, PageRequest.of(0, 10));
        PageResponse<OrderListItemResponse> adminOrders = orderService.listAdmin(
                OrderStatus.PENDING,
                firstCustomer,
                first.orderNumber(),
                null,
                null,
                null,
                null,
                PageRequest.of(0, 10)
        );

        assertThat(customerOrders.content()).hasSize(1);
        assertThat(customerOrders.content().get(0).customerId()).isEqualTo(firstCustomer);
        assertThat(adminOrders.content()).hasSize(1);
        assertThat(adminOrders.content().get(0).orderNumber()).isEqualTo(first.orderNumber());
    }

    @Test
    void adminFiltersByTotalRange() {
        UUID customerId = UUID.randomUUID();
        OrderResponse small = createPersistedOrder("SKU-TOTAL-SMALL", customerId, BigDecimal.valueOf(1000, 2));
        createPersistedOrder("SKU-TOTAL-LARGE", UUID.randomUUID(), BigDecimal.valueOf(6000, 2));

        PageResponse<OrderListItemResponse> adminOrders = orderService.listAdmin(
                OrderStatus.PENDING,
                null,
                null,
                null,
                null,
                BigDecimal.valueOf(900, 2),
                BigDecimal.valueOf(2000, 2),
                PageRequest.of(0, 10)
        );

        assertThat(adminOrders.content()).hasSize(1);
        assertThat(adminOrders.content().get(0).id()).isEqualTo(small.id());
    }

    @Test
    void mapsCheckoutPreviewFailureToExternalServiceUnavailable() {
        UUID customerId = UUID.randomUUID();
        when(cartClient.getCheckoutPreview(customerId)).thenThrow(new RuntimeException("cart unavailable"));

        assertThatThrownBy(() -> orderService.createOrder(createRequest(customerId)))
                .isInstanceOf(ExternalServiceUnavailableException.class);

        verify(inventoryClient, never()).reserve(any(ReserveStockRequest.class));
    }

    private OrderResponse createPersistedOrder(String sku, UUID customerId) {
        return createPersistedOrder(sku, customerId, BigDecimal.valueOf(2500, 2));
    }

    private OrderResponse createPersistedOrder(String sku, UUID customerId, BigDecimal unitPrice) {
        UUID cartId = UUID.randomUUID();
        CartItemResponse item = cartItem(sku, 1, unitPrice);
        when(cartClient.getCheckoutPreview(customerId)).thenReturn(validPreview(cartId, customerId, List.of(item)));
        return orderService.createOrder(createRequest(customerId));
    }

    private CreateOrderRequest createRequest(UUID customerId) {
        return createRequest(customerId, "checkout-" + customerId, "web-" + customerId);
    }

    private CreateOrderRequest createRequest(UUID customerId, String idempotencyKey, String externalReference) {
        return new CreateOrderRequest(
                customerId,
                new AddressSnapshotRequest(
                        "Ada Lovelace",
                        "+90 555 000 00 00",
                        "Istanbul",
                        "Kadikoy",
                        "Moda Caddesi No: 42",
                        "34710",
                        "TR"
                ),
                "Leave at reception",
                OrderSource.WEB,
                idempotencyKey,
                externalReference
        );
    }

    private CheckoutPreviewResponse validPreview(UUID cartId, UUID customerId, List<CartItemResponse> items) {
        BigDecimal selectedTotal = items.stream()
                .map(CartItemResponse::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CheckoutPreviewResponse(
                cartId,
                customerId,
                "TRY",
                items,
                items.size(),
                selectedTotal,
                new CartValidationResponse(cartId, customerId, true, items.size(), items.size(), selectedTotal, selectedTotal, List.of())
        );
    }

    private CheckoutPreviewResponse invalidPreview(UUID cartId, UUID customerId, CartItemResponse item) {
        return new CheckoutPreviewResponse(
                cartId,
                customerId,
                "TRY",
                List.of(item),
                1,
                item.lineTotal(),
                new CartValidationResponse(
                        cartId,
                        customerId,
                        false,
                        1,
                        1,
                        item.lineTotal(),
                        item.lineTotal(),
                        List.of(new CartValidationIssueResponse(item.id(), item.productVariantId(), item.sku(), "INSUFFICIENT_STOCK", "Insufficient available stock"))
                )
        );
    }

    private CartItemResponse cartItem(String sku, int quantity, BigDecimal unitPrice) {
        return cartItem(sku, quantity, unitPrice, "Cotton Shirt");
    }

    private CartItemResponse cartItem(String sku, int quantity, BigDecimal unitPrice, String productName) {
        UUID variantId = UUID.randomUUID();
        return new CartItemResponse(
                UUID.randomUUID(),
                UUID.randomUUID(),
                variantId,
                sku,
                productName,
                "cotton-shirt",
                "Black / M",
                "https://assets.ascmoda.test/cotton-shirt.jpg",
                "Black",
                "M",
                unitPrice,
                quantity,
                true,
                unitPrice.multiply(BigDecimal.valueOf(quantity)),
                0L,
                Instant.now(),
                Instant.now()
        );
    }

    private StockReservationResponse reservationResponse(UUID variantId, String sku, int quantity, String reservationKey) {
        return new StockReservationResponse(
                UUID.randomUUID(),
                UUID.randomUUID(),
                variantId,
                sku,
                ReferenceType.ORDER,
                "ORD-TEST",
                reservationKey,
                quantity,
                StockReservationStatus.ACTIVE,
                Instant.now().plusSeconds(1800),
                0L,
                Instant.now(),
                Instant.now()
        );
    }
}
