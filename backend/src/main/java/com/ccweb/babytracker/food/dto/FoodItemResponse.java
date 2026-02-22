package com.ccweb.babytracker.food.dto;

public record FoodItemResponse(Long id, String name, String category, int recommendedAgeMonths, String notes) {}
