package com.ascmoda.inventory.application.service;

import com.ascmoda.inventory.domain.model.InventoryItem;
import com.ascmoda.inventory.domain.model.StockReservation;
import com.ascmoda.shared.kernel.event.EventEnvelope;
import com.ascmoda.shared.kernel.event.EventTypes;
import com.ascmoda.shared.kernel.event.inventory.InventoryLowStockEvent;
import com.ascmoda.shared.kernel.event.inventory.InventoryStockConsumedEvent;
import com.ascmoda.shared.kernel.event.inventory.InventoryStockReleasedEvent;
import com.ascmoda.shared.kernel.event.inventory.InventoryStockReservedEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class InventoryEventFactory {

    private static final String SOURCE_SERVICE = "inventory-service";

    private final ObjectMapper objectMapper;

    public InventoryEventFactory(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public EventDocument stockReserved(InventoryItem item, StockReservation reservation, int changedQuantity) {
        UUID eventId = UUID.randomUUID();
        Instant occurredAt = Instant.now();
        InventoryStockReservedEvent payload = new InventoryStockReservedEvent(
                item.getId(),
                item.getProductVariantId(),
                item.getSku(),
                item.getQuantityOnHand(),
                item.getReservedQuantity(),
                item.availableQuantity(),
                changedQuantity,
                item.getLowStockThreshold(),
                reservation.getReferenceType().name(),
                reservation.getReferenceId(),
                reservation.getId(),
                occurredAt,
                SOURCE_SERVICE
        );
        return toDocument(eventId, EventTypes.INVENTORY_STOCK_RESERVED, occurredAt, correlationId(reservation), payload);
    }

    public EventDocument stockReleased(InventoryItem item, StockReservation reservation, int changedQuantity) {
        UUID eventId = UUID.randomUUID();
        Instant occurredAt = Instant.now();
        InventoryStockReleasedEvent payload = new InventoryStockReleasedEvent(
                item.getId(),
                item.getProductVariantId(),
                item.getSku(),
                item.getQuantityOnHand(),
                item.getReservedQuantity(),
                item.availableQuantity(),
                changedQuantity,
                item.getLowStockThreshold(),
                reservation.getReferenceType().name(),
                reservation.getReferenceId(),
                reservation.getId(),
                occurredAt,
                SOURCE_SERVICE
        );
        return toDocument(eventId, EventTypes.INVENTORY_STOCK_RELEASED, occurredAt, correlationId(reservation), payload);
    }

    public EventDocument stockConsumed(InventoryItem item, StockReservation reservation, int changedQuantity) {
        UUID eventId = UUID.randomUUID();
        Instant occurredAt = Instant.now();
        InventoryStockConsumedEvent payload = new InventoryStockConsumedEvent(
                item.getId(),
                item.getProductVariantId(),
                item.getSku(),
                item.getQuantityOnHand(),
                item.getReservedQuantity(),
                item.availableQuantity(),
                changedQuantity,
                item.getLowStockThreshold(),
                reservation.getReferenceType().name(),
                reservation.getReferenceId(),
                reservation.getId(),
                occurredAt,
                SOURCE_SERVICE
        );
        return toDocument(eventId, EventTypes.INVENTORY_STOCK_CONSUMED, occurredAt, correlationId(reservation), payload);
    }

    public EventDocument lowStock(InventoryItem item, StockReservation reservation) {
        UUID eventId = UUID.randomUUID();
        Instant occurredAt = Instant.now();
        InventoryLowStockEvent payload = new InventoryLowStockEvent(
                item.getId(),
                item.getProductVariantId(),
                item.getSku(),
                item.getQuantityOnHand(),
                item.getReservedQuantity(),
                item.availableQuantity(),
                item.getLowStockThreshold(),
                reservation == null ? null : reservation.getReferenceType().name(),
                reservation == null ? null : reservation.getReferenceId(),
                reservation == null ? null : reservation.getId(),
                occurredAt,
                SOURCE_SERVICE
        );
        return toDocument(eventId, EventTypes.INVENTORY_STOCK_LOW, occurredAt, correlationId(item, reservation), payload);
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
            throw new IllegalStateException("Inventory event payload could not be serialized", ex);
        }
    }

    private String correlationId(StockReservation reservation) {
        return reservation.getReferenceType() + ":" + reservation.getReferenceId();
    }

    private String correlationId(InventoryItem item, StockReservation reservation) {
        if (reservation != null) {
            return correlationId(reservation);
        }
        return "INVENTORY:" + item.getId();
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
