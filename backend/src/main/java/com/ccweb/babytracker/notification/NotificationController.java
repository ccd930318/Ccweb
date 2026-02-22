package com.ccweb.babytracker.notification;

import com.ccweb.babytracker.notification.dto.NotificationResponse;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public List<NotificationResponse> list(Authentication auth) {
        return notificationService.list(UUID.fromString(auth.getName()));
    }

    @PatchMapping("/{id}/read")
    public NotificationResponse markRead(@PathVariable UUID id, Authentication auth) {
        return notificationService.markRead(id, UUID.fromString(auth.getName()));
    }

    @GetMapping("/unread-count")
    public Map<String, Long> unreadCount(Authentication auth) {
        return notificationService.unreadCount(UUID.fromString(auth.getName()));
    }
}
