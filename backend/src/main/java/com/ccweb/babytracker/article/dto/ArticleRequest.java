package com.ccweb.babytracker.article.dto;

import jakarta.validation.constraints.NotBlank;

public record ArticleRequest(@NotBlank String title, @NotBlank String content, String category) {}
