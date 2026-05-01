package com.ascmoda.notification.infrastructure.sender;

import com.ascmoda.notification.application.sender.NotificationSender;
import com.ascmoda.notification.domain.model.NotificationMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "ascmoda.notification.sender.log", name = "enabled", havingValue = "true", matchIfMissing = true)
public class LogNotificationSender implements NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(LogNotificationSender.class);

    @Override
    public void send(NotificationMessage message) {
        log.info("Simulated notification send type={} channel={} recipient={} referenceType={} referenceId={} subject={}",
                message.getNotificationType(),
                message.getChannel(),
                message.getRecipient(),
                message.getReferenceType(),
                message.getReferenceId(),
                message.getSubject());
    }
}
