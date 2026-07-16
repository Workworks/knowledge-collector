package com.example.knowledgecollector.application.crawl;

import java.time.OffsetDateTime;

public record CrawlTaskView(Long id, String taskNo, Long retryOfTaskId, String triggerType,
        Long sourceId, String sourceName, String status, boolean cancelRequested,
        int discoveredCount, int createdCount, int duplicateCount, int failedCount,
        String errorCode, String errorMessage, OffsetDateTime startedAt,
        OffsetDateTime finishedAt, Long durationMillis, OffsetDateTime createdAt) {}
