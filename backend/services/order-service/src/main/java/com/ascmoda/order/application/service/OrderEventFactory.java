package com.ascmoda.order.application.service;

import com.ascmoda.order.domain.model.Order;
import com.ascmoda.shared.kernel.event.EventEnvelope;
import com.ascmoda.shared.kernel.event.EventTypes;
import com.ascmoda.shared.kernel.event.order.OrderCancelledEvent;
import com.ascmoda.shared.kernel.event.order.OrderConfirmedEvent;
import com.ascmoda.shared.kernel.event.order.OrderCreatedEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class OrderEventFactory {

    private static final String SOURCE_SERVICE = "order-service";

    private final ObjectMapper objectMapper;

    public OrderEventFactory(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public EventDocument orderCreated(Order order) {
        UUID eventId = UUID.randomUUID();
        Instant occurredAt = Instant.now();
        OrderCreatedEvent payload = new OrderCreatedEvent(
                order.getId(),
                order.getOrderNumber(),
                order.getCustomerId(),
                order.getTotalAmount(),
                order.getCurrency(),
                order.getStatus().name(),
                order.getCreatedAt(),
                order.getCustomerSnapshot().getFullName(),
                order.getCustomerSnapshot().getPhoneNumber(),
                order.getExternalReference(),
                order.getPaymentReference()
        );
        return toDocument(eventId, EventTypes.ORDER_CREATED, occurredAt, order.getOrderNumber(), payload);
    }

    public EventDocument orderConfirmed(Order order) {
        UUID eventId = UUID.randomUUID();
        Instant occurredAt = Instant.now();
        OrderConfirmedEvent payload = new OrderConfirmedEvent(
                order.getId(),
                order.getOrderNumber(),
                order.getCustomerId(),
                order.getTotalAmount(),
                order.getCurrency(),
                order.getStatus().name(),
                order.getConfirmedAt(),
                order.getCustomerSnapshot().getFullName(),
                order.getCustomerSnapshot().getPhoneNumber(),
                order.getExternalReference(),
                order.getPaymentReference()
        );
        return toDocument(eventId, EventTypes.ORDER_CONFIRMED, occurredAt, order.getOrderNumber(), payload);
    }

    public EventDocument orderCancelled(Order order) {
        UUID eventId = UUID.randomUUID();
        Instant occurredAt = Instant.now();
        OrderCancelledEvent payload = new OrderCancelledEvent(
                order.getId(),
                order.getOrderNumber(),
                order.getCustomerId(),
                order.getTotalAmount(),
                order.getCurrency(),
                order.getStatus().name(),
                order.getCancelledAt(),
                order.getCancellationReason(),
                order.getCustomerSnapshot().getFullName(),
                order.getCustomerSnapshot().getPhoneNumber(),
                order.getExternalReference(),
                order.getPaymentReference()
        );
        return toDocument(eventId, EventTypes.ORDER_CANCELLED, occurredAt, order.getOrderNumber(), payload);
    }

    private EventDocument toDocument(UUID eventId, String eventType, Instant occurredAt, String correlationId,
                                     Object payload) {
        EventEnvelope<Object> envelope = new EventEnvelope<>(
                eventId,
                eventType,
                occurredAt,
                SOURCE_SERVICE,
                correlationId,
                payload
        );
        try {
            return new EventDocument(eventId, eventType, occurredAt, correlationId, objectMapper.writeValueAsString(envelope));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Order event payload could not be serialized", ex);
        }
    }

    public record EventDocument(
            UUID eventId,
            String eventType,
            Instant occurredAt,
            String correlationId,
            String payloadJson
    ) {
    }
}
