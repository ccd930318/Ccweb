package com.ccweb.babytracker.article.dto;

import java.time.Instant;
import java.util.UUID;

public record ArticleResponse(UUID id, String title, String content, String category, Instant createdAt) {}
