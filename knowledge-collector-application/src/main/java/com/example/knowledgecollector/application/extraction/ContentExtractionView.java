package com.example.knowledgecollector.application.extraction;
import java.time.OffsetDateTime;
public record ContentExtractionView(Long id, Long articleId, String requestedUrl, String finalUrl, String method,
 String providerId, String status, String pageTitle, String author, OffsetDateTime publishedAt, int contentLength,
 String contentHtml, String contentText, String rawHtml, byte[] screenshot, long durationMillis, String errorMessage,
 Long retryOfId, OffsetDateTime createdAt, OffsetDateTime finishedAt) {}
