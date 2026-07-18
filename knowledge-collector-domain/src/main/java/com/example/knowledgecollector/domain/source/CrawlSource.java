package com.example.knowledgecollector.domain.source;

import java.time.OffsetDateTime;
import java.util.Set;

public record CrawlSource(
        Long id,
        String code,
        String name,
        SourceType type,
        String homeUrl,
        String feedUrl,
        String language,
        String charset,
        String userAgent,
        int timeoutSeconds,
        int maxRetries,
        long requestIntervalMillis,
        boolean obeyRobots,
        boolean fetchFullContent,
        boolean summaryOnly,
        boolean saveSnapshot,
        boolean enabled,
        OffsetDateTime lastSuccessAt,
        OffsetDateTime lastFailureAt,
        int consecutiveFailures,
        String healthStatus,
        OffsetDateTime lastHealthCheckedAt,
        String lastHealthMessage,
        String notes,
        Set<Long> topicIds,
        long version,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
