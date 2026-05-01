package com.ascmoda.notification.application.service;

import com.ascmoda.notification.application.dto.NotificationContent;
import com.ascmoda.notification.application.dto.ParsedNotificationEvent;
import com.ascmoda.notification.domain.exception.UnsupportedNotificationEventException;
import com.ascmoda.notification.domain.model.NotificationChannel;
import com.ascmoda.notification.domain.model.NotificationType;
import com.ascmoda.shared.kernel.event.EventTypes;
import com.ascmoda.shared.kernel.event.inventory.InventoryLowStockEvent;
import com.ascmoda.shared.kernel.event.inventory.InventoryStockConsumedEvent;
import com.ascmoda.shared.kernel.event.inventory.InventoryStockReleasedEvent;
import com.ascmoda.shared.kernel.event.inventory.InventoryStockReservedEvent;
import com.ascmoda.shared.kernel.event.order.OrderCancelledEvent;
import com.ascmoda.shared.kernel.event.order.OrderConfirmedEvent;
import com.ascmoda.shared.kernel.event.order.OrderCreatedEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class NotificationContentBuilder {

    private final String opsRecipient;

    public NotificationContentBuilder(@Value("${ascmoda.notification.recipient.ops:ops@ascmoda.local}") String opsRecipient) {
        this.opsRecipient = opsRecipient;
    }

    public NotificationContent build(ParsedNotificationEvent event) {
        return switch (event.eventType()) {
            case EventTypes.ORDER_CREATED -> orderCreated((OrderCreatedEvent) event.payload());
            case EventTypes.ORDER_CONFIRMED -> orderConfirmed((OrderConfirmedEvent) event.payload());
            case EventTypes.ORDER_CANCELLED -> orderCancelled((OrderCancelledEvent) event.payload());
            case EventTypes.INVENTORY_STOCK_RESERVED ->
                    stockReserved((InventoryStockReservedEvent) event.payload());
            case EventTypes.INVENTORY_STOCK_RELEASED ->
                    stockReleased((InventoryStockReleasedEvent) event.payload());
            case EventTypes.INVENTORY_STOCK_CONSUMED ->
                    stockConsumed((InventoryStockConsumedEvent) event.payload());
            case EventTypes.INVENTORY_STOCK_LOW -> lowStockAlert((InventoryLowStockEvent) event.payload());
            default -> throw new UnsupportedNotificationEventException("Unsupported event type: " + event.eventType());
        };
    }

    private NotificationContent orderCreated(OrderCreatedEvent event) {
        String subject = "Siparisiniz alindi: " + event.orderNumber();
        String body = "Merhaba " + customerName(event.customerFullName()) + ", "
                + event.orderNumber() + " numarali siparisiniz alindi. Toplam: "
                + amount(event.totalAmount(), event.currency()) + ".";
        return orderContent(NotificationType.ORDER_CREATED, event.phoneNumber(), subject, body, event.orderId().toString());
    }

    private NotificationContent orderConfirmed(OrderConfirmedEvent event) {
        String subject = "Siparisiniz onaylandi: " + event.orderNumber();
        String body = "Merhaba " + customerName(event.customerFullName()) + ", "
                + event.orderNumber() + " numarali siparisiniz onaylandi. Toplam: "
                + amount(event.totalAmount(), event.currency()) + ".";
        return orderContent(NotificationType.ORDER_CONFIRMED, event.phoneNumber(), subject, body, event.orderId().toString());
    }

    private NotificationContent orderCancelled(OrderCancelledEvent event) {
        String reason = event.cancellationReason() == null || event.cancellationReason().isBlank()
                ? ""
                : " Iptal nedeni: " + event.cancellationReason() + ".";
        String subject = "Siparisiniz iptal edildi: " + event.orderNumber();
        String body = "Merhaba " + customerName(event.customerFullName()) + ", "
                + event.orderNumber() + " numarali siparisiniz iptal edildi." + reason;
        return orderContent(NotificationType.ORDER_CANCELLED, event.phoneNumber(), subject, body, event.orderId().toString());
    }

    private NotificationContent stockReserved(InventoryStockReservedEvent event) {
        String subject = "Stok rezerve edildi: " + event.sku();
        String body = "SKU " + event.sku() + " icin " + event.changedQuantity()
                + " adet stok rezerve edildi. Kullanilabilir stok: " + event.availableQuantity()
                + "." + referencePart(event.referenceType(), event.referenceId());
        return inventoryContent(NotificationType.STOCK_RESERVED, subject, body, event.inventoryItemId().toString());
    }

    private NotificationContent stockReleased(InventoryStockReleasedEvent event) {
        String subject = "Stok rezervasyonu serbest birakildi: " + event.sku();
        String body = "SKU " + event.sku() + " icin " + event.changedQuantity()
                + " adet rezervasyon serbest birakildi. Kullanilabilir stok: " + event.availableQuantity()
                + "." + referencePart(event.referenceType(), event.referenceId());
        return inventoryContent(NotificationType.STOCK_RELEASED, subject, body, event.inventoryItemId().toString());
    }

    private NotificationContent stockConsumed(InventoryStockConsumedEvent event) {
        String subject = "Stok tuketildi: " + event.sku();
        String body = "SKU " + event.sku() + " icin " + event.changedQuantity()
                + " adet stok tuketildi. Kalan kullanilabilir stok: " + event.availableQuantity()
                + "." + referencePart(event.referenceType(), event.referenceId());
        return inventoryContent(NotificationType.STOCK_CONSUMED, subject, body, event.inventoryItemId().toString());
    }

    private NotificationContent lowStockAlert(InventoryLowStockEvent event) {
        String subject = "Dusuk stok uyarisi: " + event.sku();
        String body = "SKU " + event.sku() + " dusuk stok seviyesine indi. Kullanilabilir stok: "
                + event.availableQuantity() + ", esik: " + event.lowStockThreshold()
                + "." + referencePart(event.referenceType(), event.referenceId());
        return inventoryContent(NotificationType.LOW_STOCK_ALERT, subject, body, event.inventoryItemId().toString());
    }

    private NotificationContent orderContent(NotificationType type, String recipient, String subject, String body,
                                             String referenceId) {
        return new NotificationContent(
                type,
                NotificationChannel.SMS,
                recipient,
                subject,
                body,
                "ORDER",
                referenceId
        );
    }

    private NotificationContent inventoryContent(NotificationType type, String subject, String body, String referenceId) {
        return new NotificationContent(
                type,
                NotificationChannel.EMAIL,
                opsRecipient,
                subject,
                body,
                "INVENTORY",
                referenceId
        );
    }

    private String customerName(String value) {
        return value == null || value.isBlank() ? "musterimiz" : value;
    }

    private String amount(BigDecimal amount, String currency) {
        return amount + " " + currency;
    }

    private String referencePart(String referenceType, String referenceId) {
        if (referenceType == null || referenceType.isBlank() || referenceId == null || referenceId.isBlank()) {
            return "";
        }
        return " Referans: " + referenceType + "/" + referenceId + ".";
    }
}
