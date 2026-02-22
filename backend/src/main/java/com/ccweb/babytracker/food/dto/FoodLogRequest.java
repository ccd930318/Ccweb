package com.ccweb.babytracker.food.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record FoodLogRequest(@NotNull Long foodItemId, @NotNull LocalDate introducedDate, String reaction) {}
