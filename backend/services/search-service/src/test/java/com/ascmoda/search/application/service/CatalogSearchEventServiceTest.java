package com.ascmoda.search.application.service;

import com.ascmoda.search.domain.exception.SearchIndexingException;
import com.ascmoda.search.domain.exception.UnsupportedSearchEventException;
import com.ascmoda.search.domain.model.ProcessedEventStatus;
import com.ascmoda.search.domain.model.SearchProcessedEvent;
import com.ascmoda.search.domain.repository.SearchProcessedEventRepository;
import com.ascmoda.search.infrastructure.elasticsearch.ProductSearchDocument;
import com.ascmoda.shared.kernel.event.EventEnvelope;
import com.ascmoda.shared.kernel.event.EventTypes;
import com.ascmoda.shared.kernel.event.catalog.CatalogProductCreatedEvent;
import com.ascmoda.shared.kernel.event.catalog.CatalogProductDeactivatedEvent;
import com.ascmoda.shared.kernel.event.catalog.CatalogProductVariantEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CatalogSearchEventServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private final SearchProcessedEventRepository processedEventRepository = mock(SearchProcessedEventRepository.class);
    private final ProductSearchIndexService productSearchIndexService = mock(ProductSearchIndexService.class);
    private final CatalogSearchEventService service = new CatalogSearchEventService(
            new SearchEventParser(objectMapper),
            new ProductSearchDocumentFactory(),
            productSearchIndexService,
            processedEventRepository
    );

    @BeforeEach
    void setUp() {
        when(processedEventRepository.save(any(SearchProcessedEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void productCreatedEventUpsertsSearchDocumentAndMarksProcessed() throws Exception {
        UUID eventId = UUID.randomUUID();
        when(processedEventRepository.findByEventId(eventId)).thenReturn(Optional.empty());

        service.process(envelope(eventId, EventTypes.CATALOG_PRODUCT_CREATED, createdPayload("search-sneaker")));

        ArgumentCaptor<ProductSearchDocument> documentCaptor = ArgumentCaptor.forClass(ProductSearchDocument.class);
        ArgumentCaptor<SearchProcessedEvent> processedCaptor = ArgumentCaptor.forClass(SearchProcessedEvent.class);
        verify(productSearchIndexService).upsert(documentCaptor.capture());
        verify(processedEventRepository).save(processedCaptor.capture());
        assertThat(documentCaptor.getValue().getProductSlug()).isEqualTo("search-sneaker");
        assertThat(processedCaptor.getValue().getStatus()).isEqualTo(ProcessedEventStatus.PROCESSED);
    }

    @Test
    void productDeactivatedEventDeletesSearchDocument() throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        when(processedEventRepository.findByEventId(eventId)).thenReturn(Optional.empty());

        service.process(envelope(
                eventId,
                EventTypes.CATALOG_PRODUCT_DEACTIVATED,
                new CatalogProductDeactivatedEvent(productId, "archived-product", "INACTIVE", Instant.now(), "catalog-service")
        ));

        verify(productSearchIndexService).deleteByProductId(productId);
    }

    @Test
    void duplicateProcessedEventDoesNotTouchIndexAgain() throws Exception {
        UUID eventId = UUID.randomUUID();
        SearchProcessedEvent processed = new SearchProcessedEvent(
                eventId,
                EventTypes.CATALOG_PRODUCT_CREATED,
                "product-1",
                "PRODUCT",
                "product-1"
        );
        processed.markProcessed(Instant.now());
        when(processedEventRepository.findByEventId(eventId)).thenReturn(Optional.of(processed));

        service.process(envelope(eventId, EventTypes.CATALOG_PRODUCT_CREATED, createdPayload("duplicate-product")));

        verify(productSearchIndexService, never()).upsert(any(ProductSearchDocument.class));
    }

    @Test
    void indexingFailureMarksEventFailed() throws Exception {
        UUID eventId = UUID.randomUUID();
        when(processedEventRepository.findByEventId(eventId)).thenReturn(Optional.empty());
        doThrow(new SearchIndexingException("Elasticsearch product upsert failed", new RuntimeException("down")))
                .when(productSearchIndexService)
                .upsert(any(ProductSearchDocument.class));

        assertThatThrownBy(() -> service.process(envelope(
                eventId,
                EventTypes.CATALOG_PRODUCT_CREATED,
                createdPayload("failing-product")
        ))).isInstanceOf(SearchIndexingException.class);

        ArgumentCaptor<SearchProcessedEvent> processedCaptor = ArgumentCaptor.forClass(SearchProcessedEvent.class);
        verify(processedEventRepository).save(processedCaptor.capture());
        assertThat(processedCaptor.getValue().getStatus()).isEqualTo(ProcessedEventStatus.FAILED);
    }

    @Test
    void unsupportedEventTypeIsRejected() throws Exception {
        UUID eventId = UUID.randomUUID();

        assertThatThrownBy(() -> service.process(envelope(
                eventId,
                "catalog.category.updated",
                createdPayload("unsupported-product")
        ))).isInstanceOf(UnsupportedSearchEventException.class);
    }

    private String envelope(UUID eventId, String eventType, Object payload) throws JsonProcessingException {
        EventEnvelope<Object> envelope = new EventEnvelope<>(
                eventId,
                eventType,
                Instant.now(),
                "catalog-service",
                "product-1",
                payload
        );
        return objectMapper.writeValueAsString(envelope);
    }

    private CatalogProductCreatedEvent createdPayload(String slug) {
        UUID productId = UUID.randomUUID();
        return new CatalogProductCreatedEvent(
                productId,
                "Search Sneaker",
                slug,
                "Short search text",
                "Long search text",
                UUID.randomUUID(),
                "Shoes",
                "shoes",
                "ACTIVE",
                BigDecimal.valueOf(1299, 2),
                BigDecimal.valueOf(1499, 2),
                List.of(new CatalogProductVariantEvent(UUID.randomUUID(), "SKU-SEARCH-1", "Black", "M",
                        BigDecimal.valueOf(1299, 2), true)),
                "https://cdn.ascmoda.local/search-sneaker.jpg",
                "search sneaker shoes black",
                Instant.now(),
                "catalog-service"
        );
    }
}
