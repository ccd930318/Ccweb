package com.ccweb.babytracker.growth.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record GrowthResponse(
        UUID id,
        LocalDate measurementDate,
        BigDecimal weightKg,
        BigDecimal heightCm,
        BigDecimal headCm,
        Double weightPercentile,
        Double heightPercentile,
        Double headPercentile
) {}
