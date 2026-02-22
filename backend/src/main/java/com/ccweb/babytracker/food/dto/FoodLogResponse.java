package com.ccweb.babytracker.food.dto;

import java.time.LocalDate;
import java.util.UUID;

public record FoodLogResponse(UUID id, String foodName, String category, LocalDate introducedDate, String reaction) {}
