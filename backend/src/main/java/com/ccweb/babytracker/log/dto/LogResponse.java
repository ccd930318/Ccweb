package com.ccweb.babytracker.log.dto;

import java.time.Instant;
import java.util.UUID;

public record LogResponse(UUID id, String type, Instant occurredAt, String notes) {}
