package com.ascmoda.notification.application.service;

import com.ascmoda.notification.application.dto.NotificationContent;
import com.ascmoda.notification.application.dto.ParsedNotificationEvent;
import com.ascmoda.notification.application.sender.NotificationSender;
import com.ascmoda.notification.controller.dto.NotificationResponse;
import com.ascmoda.notification.controller.dto.PageResponse;
import com.ascmoda.notification.domain.exception.InvalidNotificationStateException;
import com.ascmoda.notification.domain.exception.NotificationNotFoundException;
import com.ascmoda.notification.domain.model.NotificationChannel;
import com.ascmoda.notification.domain.model.NotificationMessage;
import com.ascmoda.notification.domain.model.NotificationStatus;
import com.ascmoda.notification.domain.model.NotificationType;
import com.ascmoda.notification.domain.repository.NotificationMessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationMessageRepository notificationMessageRepository;
    private final NotificationEventParser notificationEventParser;
    private final NotificationContentBuilder notificationContentBuilder;
    private final NotificationSender notificationSender;
    private final int maxRetryAttempts;

    public NotificationService(NotificationMessageRepository notificationMessageRepository,
                               NotificationEventParser notificationEventParser,
                               NotificationContentBuilder notificationContentBuilder,
                               NotificationSender notificationSender,
                               @Value("${ascmoda.notification.retry.max-attempts:3}") int maxRetryAttempts) {
        this.notificationMessageRepository = notificationMessageRepository;
        this.notificationEventParser = notificationEventParser;
        this.notificationContentBuilder = notificationContentBuilder;
        this.notificationSender = notificationSender;
        this.maxRetryAttempts = maxRetryAttempts;
    }

    @Transactional
    public NotificationResponse process(String message) {
        ParsedNotificationEvent event = notificationEventParser.parse(message);
        return notificationMessageRepository.findByEventId(event.eventId())
                .map(existing -> {
                    log.info("Skipped duplicate notification event eventId={} correlationId={}",
                            event.eventId(), event.correlationId());
                    return toResponse(existing);
                })
                .orElseGet(() -> createAndSend(event));
    }

    @Transactional
    public NotificationResponse retry(UUID id) {
        NotificationMessage message = getEntity(id);
        if (!message.isRetryable()) {
            throw new InvalidNotificationStateException("Only failed notifications can be retried");
        }
        if (message.getRetryCount() >= maxRetryAttempts) {
            throw new InvalidNotificationStateException("Notification retry limit has been reached");
        }

        message.markPending();
        send(message);
        return toResponse(message);
    }

    @Transactional(readOnly = true)
    public NotificationResponse get(UUID id) {
        return toResponse(getEntity(id));
    }

    @Transactional(readOnly = true)
    public NotificationResponse getByEventId(UUID eventId) {
        return notificationMessageRepository.findByEventId(eventId)
                .map(this::toResponse)
                .orElseThrow(() -> new NotificationNotFoundException("Notification not found for event: " + eventId));
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> list(NotificationStatus status, NotificationType type,
                                                   NotificationChannel channel, String referenceType,
                                                   String referenceId, Instant createdFrom, Instant createdTo,
                                                   Pageable pageable) {
        Page<NotificationResponse> page = notificationMessageRepository.findAll(
                specification(status, type, channel, referenceType, referenceId, createdFrom, createdTo),
                pageable
        ).map(this::toResponse);
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }

    private NotificationResponse createAndSend(ParsedNotificationEvent event) {
        NotificationContent content = notificationContentBuilder.build(event);
        NotificationMessage message = notificationMessageRepository.save(new NotificationMessage(
                event.eventId(),
                event.correlationId(),
                content.type(),
                content.channel(),
                content.recipient(),
                content.subject(),
                content.body(),
                content.referenceType(),
                content.referenceId(),
                event.sourceService(),
                event.payloadJson()
        ));

        send(message);
        log.info("Processed notification event eventId={} type={} status={} correlationId={}",
                event.eventId(), content.type(), message.getStatus(), event.correlationId());
        return toResponse(message);
    }

    private void send(NotificationMessage message) {
        try {
            notificationSender.send(message);
            message.markSent(Instant.now());
        } catch (RuntimeException ex) {
            message.markFailed(ex.getMessage());
            log.warn("Notification send failed id={} eventId={} retryCount={}",
                    message.getId(), message.getEventId(), message.getRetryCount());
        }
    }

    private NotificationMessage getEntity(UUID id) {
        return notificationMessageRepository.findById(id)
                .orElseThrow(() -> new NotificationNotFoundException("Notification not found: " + id));
    }

    private Specification<NotificationMessage> specification(NotificationStatus status, NotificationType type,
                                                             NotificationChannel channel, String referenceType,
                                                             String referenceId, Instant createdFrom,
                                                             Instant createdTo) {
        Specification<NotificationMessage> specification = (root, query, criteriaBuilder) -> criteriaBuilder.conjunction();
        if (status != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("status"), status));
        }
        if (type != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("notificationType"), type));
        }
        if (channel != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("channel"), channel));
        }
        if (referenceType != null && !referenceType.isBlank()) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("referenceType"), referenceType.trim()));
        }
        if (referenceId != null && !referenceId.isBlank()) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("referenceId"), referenceId.trim()));
        }
        if (createdFrom != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), createdFrom));
        }
        if (createdTo != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), createdTo));
        }
        return specification;
    }

    private NotificationResponse toResponse(NotificationMessage message) {
        return new NotificationResponse(
                message.getId(),
                message.getEventId(),
                message.getCorrelationId(),
                message.getNotificationType(),
                message.getChannel(),
                message.getRecipient(),
                message.getSubject(),
                message.getBody(),
                message.getStatus(),
                message.getReferenceType(),
                message.getReferenceId(),
                message.getSourceService(),
                message.getFailureReason(),
                message.getRetryCount(),
                message.getProcessedAt(),
                message.getSentAt(),
                message.getVersion(),
                message.getCreatedAt(),
                message.getUpdatedAt()
        );
    }
}
