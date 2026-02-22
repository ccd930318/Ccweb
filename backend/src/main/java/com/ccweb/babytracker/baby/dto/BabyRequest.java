package com.ccweb.babytracker.baby.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record BabyRequest(
        @NotBlank String name,
        @NotNull LocalDate birthDate,
        String gender
) {}
