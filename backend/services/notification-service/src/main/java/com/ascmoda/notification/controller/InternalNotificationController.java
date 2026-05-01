package com.ascmoda.notification.controller;

import com.ascmoda.notification.application.service.NotificationService;
import com.ascmoda.notification.controller.dto.NotificationResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/internal/notifications")
public class InternalNotificationController {

    private final NotificationService notificationService;

    public InternalNotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/{eventId}")
    public NotificationResponse getByEventId(@PathVariable UUID eventId) {
        return notificationService.getByEventId(eventId);
    }
}
