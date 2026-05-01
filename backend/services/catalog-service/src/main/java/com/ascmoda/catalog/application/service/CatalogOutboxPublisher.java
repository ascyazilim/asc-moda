package com.ascmoda.catalog.application.service;

import com.ascmoda.catalog.domain.model.CatalogOutboxEvent;
import com.ascmoda.catalog.domain.model.OutboxStatus;
import com.ascmoda.catalog.domain.repository.CatalogOutboxEventRepository;
import com.ascmoda.shared.kernel.event.RabbitEventTopology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Component
@ConditionalOnProperty(prefix = "ascmoda.catalog.outbox", name = "enabled", havingValue = "true", matchIfMissing = true)
public class CatalogOutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(CatalogOutboxPublisher.class);

    private final CatalogOutboxEventRepository outboxEventRepository;
    private final RabbitTemplate rabbitTemplate;
    private final int batchSize;
    private final int maxRetries;

    public CatalogOutboxPublisher(CatalogOutboxEventRepository outboxEventRepository, RabbitTemplate rabbitTemplate,
                                  @Value("${ascmoda.catalog.outbox.batch-size:25}") int batchSize,
                                  @Value("${ascmoda.catalog.outbox.max-retries:5}") int maxRetries) {
        this.outboxEventRepository = outboxEventRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.batchSize = batchSize;
        this.maxRetries = maxRetries;
    }

    @Scheduled(fixedDelayString = "${ascmoda.catalog.outbox.publish-delay-ms:5000}")
    @Transactional
    public void publishPendingEvents() {
        List<CatalogOutboxEvent> events = outboxEventRepository.findByStatusInAndRetryCountLessThanOrderByOccurredAtAsc(
                List.of(OutboxStatus.PENDING, OutboxStatus.FAILED),
                maxRetries,
                PageRequest.of(0, batchSize)
        );

        for (CatalogOutboxEvent event : events) {
            publish(event);
        }
    }

    private void publish(CatalogOutboxEvent event) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitEventTopology.EXCHANGE,
                    event.getEventType(),
                    event.getPayloadJson(),
                    message -> {
                        message.getMessageProperties().setMessageId(event.getId().toString());
                        message.getMessageProperties().setCorrelationId(event.getCorrelationId());
                        message.getMessageProperties().setType(event.getEventType());
                        message.getMessageProperties().setContentType("application/json");
                        return message;
                    }
            );
            event.markPublished(Instant.now());
            log.info("Published catalog outbox event id={} eventType={} correlationId={}",
                    event.getId(), event.getEventType(), event.getCorrelationId());
        } catch (RuntimeException ex) {
            event.markFailed(ex.getMessage());
            log.warn("Failed to publish catalog outbox event id={} eventType={} retryCount={}",
                    event.getId(), event.getEventType(), event.getRetryCount());
        }
    }
}
