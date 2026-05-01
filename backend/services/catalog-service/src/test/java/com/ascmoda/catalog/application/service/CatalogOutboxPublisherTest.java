package com.ascmoda.catalog.application.service;

import com.ascmoda.catalog.domain.model.CatalogOutboxEvent;
import com.ascmoda.catalog.domain.model.OutboxStatus;
import com.ascmoda.catalog.domain.repository.CatalogOutboxEventRepository;
import com.ascmoda.shared.kernel.event.EventTypes;
import com.ascmoda.shared.kernel.event.RabbitEventTopology;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CatalogOutboxPublisherTest {

    private final CatalogOutboxEventRepository repository = mock(CatalogOutboxEventRepository.class);
    private final RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
    private final CatalogOutboxPublisher publisher = new CatalogOutboxPublisher(repository, rabbitTemplate, 10, 5);

    @Test
    void publishesPendingEventAndMarksItPublished() {
        CatalogOutboxEvent event = event();
        when(repository.findByStatusInAndRetryCountLessThanOrderByOccurredAtAsc(
                eq(List.of(OutboxStatus.PENDING, OutboxStatus.FAILED)),
                eq(5),
                any(Pageable.class)
        )).thenReturn(List.of(event));

        publisher.publishPendingEvents();

        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
        assertThat(event.getPublishedAt()).isNotNull();
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitEventTopology.EXCHANGE),
                eq(EventTypes.CATALOG_PRODUCT_CREATED),
                eq("{}"),
                any(MessagePostProcessor.class)
        );
    }

    @Test
    void failedPublishMarksEventFailedForRetry() {
        CatalogOutboxEvent event = event();
        when(repository.findByStatusInAndRetryCountLessThanOrderByOccurredAtAsc(
                eq(List.of(OutboxStatus.PENDING, OutboxStatus.FAILED)),
                eq(5),
                any(Pageable.class)
        )).thenReturn(List.of(event));
        doThrow(new AmqpException("rabbit down")).when(rabbitTemplate).convertAndSend(
                eq(RabbitEventTopology.EXCHANGE),
                eq(EventTypes.CATALOG_PRODUCT_CREATED),
                eq("{}"),
                any(MessagePostProcessor.class)
        );

        publisher.publishPendingEvents();

        assertThat(event.getStatus()).isEqualTo(OutboxStatus.FAILED);
        assertThat(event.getRetryCount()).isEqualTo(1);
        assertThat(event.getLastError()).isEqualTo("rabbit down");
    }

    private CatalogOutboxEvent event() {
        return new CatalogOutboxEvent(
                UUID.randomUUID(),
                "PRODUCT",
                UUID.randomUUID().toString(),
                EventTypes.CATALOG_PRODUCT_CREATED,
                "{}",
                Instant.now(),
                "product-1"
        );
    }
}
