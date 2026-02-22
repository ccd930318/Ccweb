package com.ccweb.babytracker.log.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record LogRequest(
        @NotBlank String type,
        @NotNull Instant occurredAt,
        String notes
) {}
