package com.ccweb.babytracker.vaccination.dto;

public record ScheduleResponse(Long id, String vaccineName, int doseNumber, int recommendedAgeMonths, String notes) {}
