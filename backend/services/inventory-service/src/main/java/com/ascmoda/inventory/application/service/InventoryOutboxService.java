package com.ascmoda.inventory.application.service;

import com.ascmoda.inventory.domain.model.InventoryItem;
import com.ascmoda.inventory.domain.model.InventoryOutboxEvent;
import com.ascmoda.inventory.domain.model.StockReservation;
import com.ascmoda.inventory.domain.repository.InventoryOutboxEventRepository;
import org.springframework.stereotype.Service;

@Service
public class InventoryOutboxService {

    private static final String INVENTORY_AGGREGATE = "INVENTORY_ITEM";

    private final InventoryOutboxEventRepository outboxEventRepository;
    private final InventoryEventFactory inventoryEventFactory;

    public InventoryOutboxService(InventoryOutboxEventRepository outboxEventRepository,
                                  InventoryEventFactory inventoryEventFactory) {
        this.outboxEventRepository = outboxEventRepository;
        this.inventoryEventFactory = inventoryEventFactory;
    }

    public void recordStockReserved(InventoryItem item, StockReservation reservation, int changedQuantity) {
        save(item, inventoryEventFactory.stockReserved(item, reservation, changedQuantity));
    }

    public void recordStockReleased(InventoryItem item, StockReservation reservation, int changedQuantity) {
        save(item, inventoryEventFactory.stockReleased(item, reservation, changedQuantity));
    }

    public void recordStockConsumed(InventoryItem item, StockReservation reservation, int changedQuantity) {
        save(item, inventoryEventFactory.stockConsumed(item, reservation, changedQuantity));
    }

    public void recordLowStock(InventoryItem item, StockReservation reservation) {
        save(item, inventoryEventFactory.lowStock(item, reservation));
    }

    private void save(InventoryItem item, InventoryEventFactory.EventDocument eventDocument) {
        outboxEventRepository.save(new InventoryOutboxEvent(
                eventDocument.eventId(),
                INVENTORY_AGGREGATE,
                item.getId().toString(),
                eventDocument.eventType(),
                eventDocument.payloadJson(),
                eventDocument.occurredAt(),
                eventDocument.correlationId()
        ));
    }
}
