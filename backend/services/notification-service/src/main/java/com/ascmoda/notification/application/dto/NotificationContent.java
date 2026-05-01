package com.ascmoda.notification.application.dto;

import com.ascmoda.notification.domain.model.NotificationChannel;
import com.ascmoda.notification.domain.model.NotificationType;

public record NotificationContent(
        NotificationType type,
        NotificationChannel channel,
        String recipient,
        String subject,
        String body,
        String referenceType,
        String referenceId
) {
}
