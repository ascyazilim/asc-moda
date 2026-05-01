package com.ascmoda.order.application.service;

import com.ascmoda.order.domain.model.Order;
import com.ascmoda.order.domain.model.OutboxEvent;
import com.ascmoda.order.domain.repository.OutboxEventRepository;
import org.springframework.stereotype.Service;

@Service
public class OrderOutboxService {

    private static final String ORDER_AGGREGATE = "ORDER";

    private final OutboxEventRepository outboxEventRepository;
    private final OrderEventFactory orderEventFactory;

    public OrderOutboxService(OutboxEventRepository outboxEventRepository, OrderEventFactory orderEventFactory) {
        this.outboxEventRepository = outboxEventRepository;
        this.orderEventFactory = orderEventFactory;
    }

    public void recordOrderCreated(Order order) {
        save(order, orderEventFactory.orderCreated(order));
    }

    public void recordOrderConfirmed(Order order) {
        save(order, orderEventFactory.orderConfirmed(order));
    }

    public void recordOrderCancelled(Order order) {
        save(order, orderEventFactory.orderCancelled(order));
    }

    private void save(Order order, OrderEventFactory.EventDocument eventDocument) {
        outboxEventRepository.save(new OutboxEvent(
                eventDocument.eventId(),
                ORDER_AGGREGATE,
                order.getId().toString(),
                eventDocument.eventType(),
                eventDocument.payloadJson(),
                eventDocument.occurredAt(),
                eventDocument.correlationId()
        ));
    }
}
