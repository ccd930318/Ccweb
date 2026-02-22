package com.ccweb.babytracker.baby.dto;

import java.time.LocalDate;
import java.util.UUID;

public record BabyResponse(
        UUID id,
        String name,
        LocalDate birthDate,
        String gender
) {}
