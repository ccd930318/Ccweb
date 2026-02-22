package com.ccweb.babytracker.notification;

import com.ccweb.babytracker.domain.notification.Notification;
import com.ccweb.babytracker.domain.notification.NotificationRepository;
import com.ccweb.babytracker.notification.dto.NotificationResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class NotificationService {

    private final NotificationRepository repo;

    public NotificationService(NotificationRepository repo) {
        this.repo = repo;
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> list(UUID userId) {
        return repo.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(this::toResponse).toList();
    }

    public NotificationResponse markRead(UUID notificationId, UUID userId) {
        Notification n = repo.findById(notificationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!n.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        n.setRead(true);
        return toResponse(n);
    }

    @Transactional(readOnly = true)
    public Map<String, Long> unreadCount(UUID userId) {
        return Map.of("count", repo.countByUserIdAndReadFalse(userId));
    }

    private NotificationResponse toResponse(Notification n) {
        return new NotificationResponse(n.getId(), n.getMessage(), n.getType(), n.isRead(), n.getCreatedAt());
    }
}
