package com.ascmoda.notification.application.service;

import com.ascmoda.notification.application.dto.NotificationContent;
import com.ascmoda.notification.application.dto.ParsedNotificationEvent;
import com.ascmoda.notification.domain.exception.UnsupportedNotificationEventException;
import com.ascmoda.notification.domain.model.NotificationChannel;
import com.ascmoda.notification.domain.model.NotificationType;
import com.ascmoda.shared.kernel.event.EventTypes;
import com.ascmoda.shared.kernel.event.order.OrderCancelledEvent;
import com.ascmoda.shared.kernel.event.order.OrderConfirmedEvent;
import com.ascmoda.shared.kernel.event.order.OrderCreatedEvent;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class NotificationContentBuilder {

    public NotificationContent build(ParsedNotificationEvent event) {
        return switch (event.eventType()) {
            case EventTypes.ORDER_CREATED -> orderCreated((OrderCreatedEvent) event.payload());
            case EventTypes.ORDER_CONFIRMED -> orderConfirmed((OrderConfirmedEvent) event.payload());
            case EventTypes.ORDER_CANCELLED -> orderCancelled((OrderCancelledEvent) event.payload());
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

    private String customerName(String value) {
        return value == null || value.isBlank() ? "musterimiz" : value;
    }

    private String amount(BigDecimal amount, String currency) {
        return amount + " " + currency;
    }
}
