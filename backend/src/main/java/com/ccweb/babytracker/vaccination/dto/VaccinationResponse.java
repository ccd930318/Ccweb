package com.ccweb.babytracker.vaccination.dto;

import java.time.LocalDate;
import java.util.UUID;

public record VaccinationResponse(
        UUID id,
        String vaccineName,
        int doseNumber,
        LocalDate administeredDate,
        String notes
) {}
