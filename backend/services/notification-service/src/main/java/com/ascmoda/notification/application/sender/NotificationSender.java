package com.ascmoda.notification.application.sender;

import com.ascmoda.notification.domain.model.NotificationMessage;

public interface NotificationSender {

    void send(NotificationMessage message);
}
