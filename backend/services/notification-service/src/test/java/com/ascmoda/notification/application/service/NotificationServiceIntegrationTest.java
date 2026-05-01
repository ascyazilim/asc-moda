package com.ascmoda.notification.application.service;

import com.ascmoda.notification.application.sender.NotificationSender;
import com.ascmoda.notification.controller.dto.NotificationResponse;
import com.ascmoda.notification.controller.dto.PageResponse;
import com.ascmoda.notification.domain.exception.UnsupportedNotificationEventException;
import com.ascmoda.notification.domain.model.NotificationChannel;
import com.ascmoda.notification.domain.model.NotificationMessage;
import com.ascmoda.notification.domain.model.NotificationStatus;
import com.ascmoda.notification.domain.model.NotificationType;
import com.ascmoda.notification.domain.repository.NotificationMessageRepository;
import com.ascmoda.shared.kernel.event.EventEnvelope;
import com.ascmoda.shared.kernel.event.EventTypes;
import com.ascmoda.shared.kernel.event.inventory.InventoryLowStockEvent;
import com.ascmoda.shared.kernel.event.inventory.InventoryStockConsumedEvent;
import com.ascmoda.shared.kernel.event.inventory.InventoryStockReleasedEvent;
import com.ascmoda.shared.kernel.event.inventory.InventoryStockReservedEvent;
import com.ascmoda.shared.kernel.event.order.OrderCancelledEvent;
import com.ascmoda.shared.kernel.event.order.OrderConfirmedEvent;
import com.ascmoda.shared.kernel.event.order.OrderCreatedEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

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
        "spring.rabbitmq.listener.simple.auto-startup=false",
        "spring.rabbitmq.listener.direct.auto-startup=false",
        "ascmoda.notification.config-source=test",
        "ascmoda.notification.recipient.ops=ops@ascmoda.local",
        "ascmoda.notification.retry.max-attempts=3"
})
@Testcontainers(disabledWithoutDocker = true)
class NotificationServiceIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private NotificationMessageRepository notificationMessageRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private NotificationSender notificationSender;

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
        notificationMessageRepository.deleteAll();
        reset(notificationSender);
    }

    @Test
    void consumesOrderCreatedEvent() throws Exception {
        String message = envelope(EventTypes.ORDER_CREATED, createdPayload("ORD-100"));

        NotificationResponse response = notificationService.process(message);

        assertThat(response.notificationType()).isEqualTo(NotificationType.ORDER_CREATED);
        assertThat(response.channel()).isEqualTo(NotificationChannel.SMS);
        assertThat(response.status()).isEqualTo(NotificationStatus.SENT);
        assertThat(response.subject()).contains("ORD-100");
        assertThat(response.sentAt()).isNotNull();
        verify(notificationSender).send(any(NotificationMessage.class));
    }

    @Test
    void consumesOrderConfirmedEvent() throws Exception {
        NotificationResponse response = notificationService.process(
                envelope(EventTypes.ORDER_CONFIRMED, confirmedPayload("ORD-101"))
        );

        assertThat(response.notificationType()).isEqualTo(NotificationType.ORDER_CONFIRMED);
        assertThat(response.status()).isEqualTo(NotificationStatus.SENT);
        assertThat(response.subject()).contains("onaylandi");
    }

    @Test
    void consumesOrderCancelledEvent() throws Exception {
        NotificationResponse response = notificationService.process(
                envelope(EventTypes.ORDER_CANCELLED, cancelledPayload("ORD-102"))
        );

        assertThat(response.notificationType()).isEqualTo(NotificationType.ORDER_CANCELLED);
        assertThat(response.status()).isEqualTo(NotificationStatus.SENT);
        assertThat(response.body()).contains("Stok tedarik edilemedi");
    }

    @Test
    void consumesInventoryStockReservedEvent() throws Exception {
        NotificationResponse response = notificationService.process(
                inventoryEnvelope(EventTypes.INVENTORY_STOCK_RESERVED, stockReservedPayload("SKU-INV-1"))
        );

        assertThat(response.notificationType()).isEqualTo(NotificationType.STOCK_RESERVED);
        assertThat(response.channel()).isEqualTo(NotificationChannel.EMAIL);
        assertThat(response.recipient()).isEqualTo("ops@ascmoda.local");
        assertThat(response.referenceType()).isEqualTo("INVENTORY");
        assertThat(response.subject()).contains("SKU-INV-1");
    }

    @Test
    void consumesInventoryStockReleasedEvent() throws Exception {
        NotificationResponse response = notificationService.process(
                inventoryEnvelope(EventTypes.INVENTORY_STOCK_RELEASED, stockReleasedPayload("SKU-INV-2"))
        );

        assertThat(response.notificationType()).isEqualTo(NotificationType.STOCK_RELEASED);
        assertThat(response.status()).isEqualTo(NotificationStatus.SENT);
        assertThat(response.body()).contains("serbest birakildi");
    }

    @Test
    void consumesInventoryStockConsumedEvent() throws Exception {
        NotificationResponse response = notificationService.process(
                inventoryEnvelope(EventTypes.INVENTORY_STOCK_CONSUMED, stockConsumedPayload("SKU-INV-3"))
        );

        assertThat(response.notificationType()).isEqualTo(NotificationType.STOCK_CONSUMED);
        assertThat(response.status()).isEqualTo(NotificationStatus.SENT);
        assertThat(response.body()).contains("tuketildi");
    }

    @Test
    void consumesInventoryLowStockEvent() throws Exception {
        NotificationResponse response = notificationService.process(
                inventoryEnvelope(EventTypes.INVENTORY_STOCK_LOW, lowStockPayload("SKU-INV-4"))
        );

        assertThat(response.notificationType()).isEqualTo(NotificationType.LOW_STOCK_ALERT);
        assertThat(response.status()).isEqualTo(NotificationStatus.SENT);
        assertThat(response.subject()).contains("Dusuk stok");
        assertThat(response.body()).contains("esik");
    }

    @Test
    void retriesFailedInventoryNotification() throws Exception {
        doThrow(new RuntimeException("ops channel down"))
                .when(notificationSender)
                .send(any(NotificationMessage.class));

        NotificationResponse failed = notificationService.process(
                inventoryEnvelope(EventTypes.INVENTORY_STOCK_LOW, lowStockPayload("SKU-INV-5"))
        );

        assertThat(failed.notificationType()).isEqualTo(NotificationType.LOW_STOCK_ALERT);
        assertThat(failed.status()).isEqualTo(NotificationStatus.FAILED);

        reset(notificationSender);
        doNothing().when(notificationSender).send(any(NotificationMessage.class));
        NotificationResponse retried = notificationService.retry(failed.id());

        assertThat(retried.status()).isEqualTo(NotificationStatus.SENT);
        assertThat(retried.retryCount()).isEqualTo(1);
    }

    @Test
    void duplicateEventIdDoesNotCreateSecondNotification() throws Exception {
        UUID eventId = UUID.randomUUID();
        String message = envelope(eventId, EventTypes.ORDER_CREATED, createdPayload("ORD-103"));

        notificationService.process(message);
        notificationService.process(message);

        assertThat(notificationMessageRepository.count()).isEqualTo(1);
        verify(notificationSender, times(1)).send(any(NotificationMessage.class));
    }

    @Test
    void senderFailureMarksNotificationFailedAndRetryCanSendIt() throws Exception {
        doThrow(new RuntimeException("provider down"))
                .when(notificationSender)
                .send(any(NotificationMessage.class));

        NotificationResponse failed = notificationService.process(envelope(EventTypes.ORDER_CREATED, createdPayload("ORD-104")));

        assertThat(failed.status()).isEqualTo(NotificationStatus.FAILED);
        assertThat(failed.failureReason()).isEqualTo("provider down");

        reset(notificationSender);
        doNothing().when(notificationSender).send(any(NotificationMessage.class));
        NotificationResponse retried = notificationService.retry(failed.id());

        assertThat(retried.status()).isEqualTo(NotificationStatus.SENT);
        assertThat(retried.retryCount()).isEqualTo(1);
    }

    @Test
    void listsNotificationsWithFilters() throws Exception {
        NotificationResponse created = notificationService.process(envelope(EventTypes.ORDER_CREATED, createdPayload("ORD-105")));
        notificationService.process(envelope(EventTypes.ORDER_CONFIRMED, confirmedPayload("ORD-106")));

        PageResponse<NotificationResponse> page = notificationService.list(
                NotificationStatus.SENT,
                NotificationType.ORDER_CREATED,
                NotificationChannel.SMS,
                "ORDER",
                created.referenceId(),
                null,
                null,
                PageRequest.of(0, 10)
        );

        assertThat(page.content()).hasSize(1);
        assertThat(page.content().get(0).id()).isEqualTo(created.id());
    }

    @Test
    void unsupportedEventTypeIsRejectedWithoutPersistingNotification() throws Exception {
        String message = envelope(UUID.randomUUID(), "inventory.low-stock", createdPayload("ORD-107"));

        assertThatThrownBy(() -> notificationService.process(message))
                .isInstanceOf(UnsupportedNotificationEventException.class);
        assertThat(notificationMessageRepository.count()).isZero();
    }

    private String envelope(String eventType, Object payload) throws JsonProcessingException {
        return envelope(UUID.randomUUID(), eventType, payload);
    }

    private String envelope(UUID eventId, String eventType, Object payload) throws JsonProcessingException {
        return envelope(eventId, eventType, "order-service", payload);
    }

    private String inventoryEnvelope(String eventType, Object payload) throws JsonProcessingException {
        return envelope(UUID.randomUUID(), eventType, "inventory-service", payload);
    }

    private String envelope(UUID eventId, String eventType, String sourceService, Object payload)
            throws JsonProcessingException {
        EventEnvelope<Object> envelope = new EventEnvelope<>(
                eventId,
                eventType,
                Instant.now(),
                sourceService,
                "corr-" + eventId,
                payload
        );
        return objectMapper.writeValueAsString(envelope);
    }

    private OrderCreatedEvent createdPayload(String orderNumber) {
        return new OrderCreatedEvent(
                UUID.randomUUID(),
                orderNumber,
                UUID.randomUUID(),
                BigDecimal.valueOf(49990, 2),
                "TRY",
                "PENDING",
                Instant.now(),
                "Ada Lovelace",
                "+90 555 000 00 00",
                "web-" + orderNumber,
                null
        );
    }

    private OrderConfirmedEvent confirmedPayload(String orderNumber) {
        return new OrderConfirmedEvent(
                UUID.randomUUID(),
                orderNumber,
                UUID.randomUUID(),
                BigDecimal.valueOf(49990, 2),
                "TRY",
                "CONFIRMED",
                Instant.now(),
                "Ada Lovelace",
                "+90 555 000 00 00",
                "web-" + orderNumber,
                null
        );
    }

    private OrderCancelledEvent cancelledPayload(String orderNumber) {
        return new OrderCancelledEvent(
                UUID.randomUUID(),
                orderNumber,
                UUID.randomUUID(),
                BigDecimal.valueOf(49990, 2),
                "TRY",
                "CANCELLED",
                Instant.now(),
                "Stok tedarik edilemedi",
                "Ada Lovelace",
                "+90 555 000 00 00",
                "web-" + orderNumber,
                null
        );
    }

    private InventoryStockReservedEvent stockReservedPayload(String sku) {
        return new InventoryStockReservedEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                sku,
                10,
                3,
                7,
                3,
                5,
                "ORDER",
                "ORD-INV-1",
                UUID.randomUUID(),
                Instant.now(),
                "inventory-service"
        );
    }

    private InventoryStockReleasedEvent stockReleasedPayload(String sku) {
        return new InventoryStockReleasedEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                sku,
                10,
                0,
                10,
                3,
                5,
                "ORDER",
                "ORD-INV-2",
                UUID.randomUUID(),
                Instant.now(),
                "inventory-service"
        );
    }

    private InventoryStockConsumedEvent stockConsumedPayload(String sku) {
        return new InventoryStockConsumedEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                sku,
                7,
                0,
                7,
                3,
                5,
                "ORDER",
                "ORD-INV-3",
                UUID.randomUUID(),
                Instant.now(),
                "inventory-service"
        );
    }

    private InventoryLowStockEvent lowStockPayload(String sku) {
        return new InventoryLowStockEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                sku,
                5,
                4,
                1,
                5,
                "ORDER",
                "ORD-INV-4",
                UUID.randomUUID(),
                Instant.now(),
                "inventory-service"
        );
    }
}
