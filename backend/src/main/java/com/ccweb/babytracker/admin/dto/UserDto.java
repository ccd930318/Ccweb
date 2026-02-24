package com.ccweb.babytracker.admin.dto;

import java.time.Instant;
import java.util.UUID;

public record UserDto(
    UUID id,
    String email,
    String name,
    String role,
    Instant createdAt
) {}
