package com.ascmoda.search.application.service;

import com.ascmoda.search.application.dto.ParsedSearchEvent;
import com.ascmoda.search.domain.model.SearchProcessedEvent;
import com.ascmoda.search.domain.repository.SearchProcessedEventRepository;
import com.ascmoda.shared.kernel.event.EventTypes;
import com.ascmoda.shared.kernel.event.catalog.CatalogProductCreatedEvent;
import com.ascmoda.shared.kernel.event.catalog.CatalogProductDeactivatedEvent;
import com.ascmoda.shared.kernel.event.catalog.CatalogProductUpdatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class CatalogSearchEventService {

    private static final Logger log = LoggerFactory.getLogger(CatalogSearchEventService.class);

    private final SearchEventParser searchEventParser;
    private final ProductSearchDocumentFactory productSearchDocumentFactory;
    private final ProductSearchIndexService productSearchIndexService;
    private final SearchProcessedEventRepository processedEventRepository;

    public CatalogSearchEventService(SearchEventParser searchEventParser,
                                     ProductSearchDocumentFactory productSearchDocumentFactory,
                                     ProductSearchIndexService productSearchIndexService,
                                     SearchProcessedEventRepository processedEventRepository) {
        this.searchEventParser = searchEventParser;
        this.productSearchDocumentFactory = productSearchDocumentFactory;
        this.productSearchIndexService = productSearchIndexService;
        this.processedEventRepository = processedEventRepository;
    }

    @Transactional
    public void process(String message) {
        ParsedSearchEvent event = searchEventParser.parse(message);
        SearchProcessedEvent processedEvent = processedEventRepository.findByEventId(event.eventId())
                .orElseGet(() -> processedEventRepository.save(new SearchProcessedEvent(
                        event.eventId(),
                        event.eventType(),
                        event.correlationId(),
                        "PRODUCT",
                        event.correlationId()
                )));

        if (processedEvent.isProcessed()) {
            log.info("Skipped duplicate catalog search event eventId={} eventType={} correlationId={}",
                    event.eventId(), event.eventType(), event.correlationId());
            return;
        }

        processedEvent.markProcessing();
        try {
            apply(event);
            processedEvent.markProcessed(Instant.now());
            log.info("Processed catalog search event eventId={} eventType={} correlationId={}",
                    event.eventId(), event.eventType(), event.correlationId());
        } catch (RuntimeException ex) {
            processedEvent.markFailed(ex.getMessage());
            log.warn("Catalog search event processing failed eventId={} eventType={} reason={}",
                    event.eventId(), event.eventType(), ex.getMessage());
            throw ex;
        }
    }

    private void apply(ParsedSearchEvent event) {
        switch (event.eventType()) {
            case EventTypes.CATALOG_PRODUCT_CREATED -> productSearchIndexService.upsert(
                    productSearchDocumentFactory.from((CatalogProductCreatedEvent) event.payload())
            );
            case EventTypes.CATALOG_PRODUCT_UPDATED -> productSearchIndexService.upsert(
                    productSearchDocumentFactory.from((CatalogProductUpdatedEvent) event.payload())
            );
            case EventTypes.CATALOG_PRODUCT_DEACTIVATED -> {
                CatalogProductDeactivatedEvent payload = (CatalogProductDeactivatedEvent) event.payload();
                productSearchIndexService.deleteByProductId(payload.productId());
            }
            default -> throw new IllegalStateException("Unsupported search event type: " + event.eventType());
        }
    }
}
