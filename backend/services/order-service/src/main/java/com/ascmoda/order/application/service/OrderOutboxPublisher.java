package com.ascmoda.order.application.service;

import com.ascmoda.order.domain.model.OutboxEvent;
import com.ascmoda.order.domain.model.OutboxStatus;
import com.ascmoda.order.domain.repository.OutboxEventRepository;
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
@ConditionalOnProperty(prefix = "ascmoda.order.outbox", name = "enabled", havingValue = "true", matchIfMissing = true)
public class OrderOutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OrderOutboxPublisher.class);

    private final OutboxEventRepository outboxEventRepository;
    private final RabbitTemplate rabbitTemplate;
    private final int batchSize;
    private final int maxRetries;

    public OrderOutboxPublisher(OutboxEventRepository outboxEventRepository, RabbitTemplate rabbitTemplate,
                                @Value("${ascmoda.order.outbox.batch-size:25}") int batchSize,
                                @Value("${ascmoda.order.outbox.max-retries:5}") int maxRetries) {
        this.outboxEventRepository = outboxEventRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.batchSize = batchSize;
        this.maxRetries = maxRetries;
    }

    @Scheduled(fixedDelayString = "${ascmoda.order.outbox.publish-delay-ms:5000}")
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> events = outboxEventRepository.findByStatusInAndRetryCountLessThanOrderByOccurredAtAsc(
                List.of(OutboxStatus.PENDING, OutboxStatus.FAILED),
                maxRetries,
                PageRequest.of(0, batchSize)
        );

        for (OutboxEvent event : events) {
            publish(event);
        }
    }

    private void publish(OutboxEvent event) {
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
            log.info("Published order outbox event id={} eventType={} correlationId={}",
                    event.getId(), event.getEventType(), event.getCorrelationId());
        } catch (RuntimeException ex) {
            event.markFailed(ex.getMessage());
            log.warn("Failed to publish order outbox event id={} eventType={} retryCount={}",
                    event.getId(), event.getEventType(), event.getRetryCount());
        }
    }
}
