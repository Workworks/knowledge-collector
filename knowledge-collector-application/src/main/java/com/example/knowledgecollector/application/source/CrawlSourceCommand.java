package com.example.knowledgecollector.application.source;

import com.example.knowledgecollector.domain.source.SourceType;

import java.util.Set;

public record CrawlSourceCommand(
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
        String notes,
        Set<Long> topicIds
) {
}
