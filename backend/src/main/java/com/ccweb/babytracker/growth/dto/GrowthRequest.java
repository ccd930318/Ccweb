package com.ccweb.babytracker.growth.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record GrowthRequest(
        @NotNull LocalDate measurementDate,
        BigDecimal weightKg,
        BigDecimal heightCm,
        BigDecimal headCm
) {}
