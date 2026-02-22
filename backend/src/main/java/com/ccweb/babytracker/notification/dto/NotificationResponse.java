package com.ccweb.babytracker.notification.dto;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(UUID id, String message, String type, boolean read, Instant createdAt) {}
