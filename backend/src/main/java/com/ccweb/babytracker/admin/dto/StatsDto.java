package com.ccweb.babytracker.admin.dto;

public record StatsDto(
    long totalUsers,
    long adminCount,
    long totalBabies
) {}
