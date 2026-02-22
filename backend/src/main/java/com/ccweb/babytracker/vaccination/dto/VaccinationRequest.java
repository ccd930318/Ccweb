package com.ccweb.babytracker.vaccination.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record VaccinationRequest(
        @NotNull Long scheduleId,
        @NotNull LocalDate administeredDate,
        String notes
) {}
