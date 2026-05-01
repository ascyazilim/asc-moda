package com.ascmoda.catalog.application.service;

import com.ascmoda.catalog.domain.model.CatalogOutboxEvent;
import com.ascmoda.catalog.domain.model.Product;
import com.ascmoda.catalog.domain.repository.CatalogOutboxEventRepository;
import org.springframework.stereotype.Service;

@Service
public class CatalogOutboxService {

    private static final String PRODUCT_AGGREGATE = "PRODUCT";

    private final CatalogOutboxEventRepository outboxEventRepository;
    private final CatalogEventFactory catalogEventFactory;

    public CatalogOutboxService(CatalogOutboxEventRepository outboxEventRepository,
                                CatalogEventFactory catalogEventFactory) {
        this.outboxEventRepository = outboxEventRepository;
        this.catalogEventFactory = catalogEventFactory;
    }

    public void recordProductCreated(Product product) {
        save(product, catalogEventFactory.productCreated(product));
    }

    public void recordProductUpdated(Product product) {
        save(product, catalogEventFactory.productUpdated(product));
    }

    public void recordProductDeactivated(Product product) {
        save(product, catalogEventFactory.productDeactivated(product));
    }

    private void save(Product product, CatalogEventFactory.EventDocument eventDocument) {
        outboxEventRepository.save(new CatalogOutboxEvent(
                eventDocument.eventId(),
                PRODUCT_AGGREGATE,
                product.getId().toString(),
                eventDocument.eventType(),
                eventDocument.payloadJson(),
                eventDocument.occurredAt(),
                eventDocument.correlationId()
        ));
    }
}
