package com.ascmoda.notification.infrastructure.rabbit;

import com.ascmoda.notification.application.service.NotificationService;
import com.ascmoda.notification.domain.exception.InvalidMessagePayloadException;
import com.ascmoda.notification.domain.exception.UnsupportedNotificationEventException;
import com.ascmoda.shared.kernel.event.RabbitEventTopology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class OrderEventListener {

    private static final Logger log = LoggerFactory.getLogger(OrderEventListener.class);

    private final NotificationService notificationService;

    public OrderEventListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @RabbitListener(queues = RabbitEventTopology.NOTIFICATION_ORDER_QUEUE)
    public void onMessage(String message) {
        try {
            notificationService.process(message);
        } catch (InvalidMessagePayloadException | UnsupportedNotificationEventException ex) {
            log.warn("Skipped invalid notification event message reason={}", ex.getMessage());
        } catch (RuntimeException ex) {
            log.error("Unexpected notification event processing failure", ex);
            throw ex;
        }
    }
}
