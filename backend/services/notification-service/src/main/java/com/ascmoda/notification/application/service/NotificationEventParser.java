package com.ascmoda.notification.application.service;

import com.ascmoda.notification.application.dto.ParsedNotificationEvent;
import com.ascmoda.notification.domain.exception.InvalidMessagePayloadException;
import com.ascmoda.notification.domain.exception.UnsupportedNotificationEventException;
import com.ascmoda.shared.kernel.event.EventTypes;
import com.ascmoda.shared.kernel.event.inventory.InventoryLowStockEvent;
import com.ascmoda.shared.kernel.event.inventory.InventoryStockConsumedEvent;
import com.ascmoda.shared.kernel.event.inventory.InventoryStockReleasedEvent;
import com.ascmoda.shared.kernel.event.inventory.InventoryStockReservedEvent;
import com.ascmoda.shared.kernel.event.order.OrderCancelledEvent;
import com.ascmoda.shared.kernel.event.order.OrderConfirmedEvent;
import com.ascmoda.shared.kernel.event.order.OrderCreatedEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class NotificationEventParser {

    private final ObjectMapper objectMapper;

    public NotificationEventParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ParsedNotificationEvent parse(String message) {
        try {
            JsonNode root = objectMapper.readTree(message);
            UUID eventId = UUID.fromString(requiredText(root, "eventId"));
            String eventType = requiredText(root, "eventType");
            Instant occurredAt = Instant.parse(requiredText(root, "occurredAt"));
            String sourceService = requiredText(root, "sourceService");
            String correlationId = requiredText(root, "correlationId");
            JsonNode payloadNode = root.get("payload");
            if (payloadNode == null || payloadNode.isNull()) {
                throw new InvalidMessagePayloadException("Event payload must be provided");
            }

            Object payload = switch (eventType) {
                case EventTypes.ORDER_CREATED -> objectMapper.treeToValue(payloadNode, OrderCreatedEvent.class);
                case EventTypes.ORDER_CONFIRMED -> objectMapper.treeToValue(payloadNode, OrderConfirmedEvent.class);
                case EventTypes.ORDER_CANCELLED -> objectMapper.treeToValue(payloadNode, OrderCancelledEvent.class);
                case EventTypes.INVENTORY_STOCK_RESERVED ->
                        objectMapper.treeToValue(payloadNode, InventoryStockReservedEvent.class);
                case EventTypes.INVENTORY_STOCK_RELEASED ->
                        objectMapper.treeToValue(payloadNode, InventoryStockReleasedEvent.class);
                case EventTypes.INVENTORY_STOCK_CONSUMED ->
                        objectMapper.treeToValue(payloadNode, InventoryStockConsumedEvent.class);
                case EventTypes.INVENTORY_STOCK_LOW -> objectMapper.treeToValue(payloadNode, InventoryLowStockEvent.class);
                default -> throw new UnsupportedNotificationEventException("Unsupported event type: " + eventType);
            };

            return new ParsedNotificationEvent(
                    eventId,
                    eventType,
                    occurredAt,
                    sourceService,
                    correlationId,
                    payload,
                    objectMapper.writeValueAsString(payloadNode)
            );
        } catch (InvalidMessagePayloadException | UnsupportedNotificationEventException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new InvalidMessagePayloadException("Notification event payload is invalid");
        } catch (Exception ex) {
            throw new InvalidMessagePayloadException("Notification event payload is invalid");
        }
    }

    private String requiredText(JsonNode root, String field) {
        JsonNode node = root.get(field);
        if (node == null || node.isNull() || node.asText().isBlank()) {
            throw new InvalidMessagePayloadException("Event " + field + " must be provided");
        }
        return node.asText();
    }
}
