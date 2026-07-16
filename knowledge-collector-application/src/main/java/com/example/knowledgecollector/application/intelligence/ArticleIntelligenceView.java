package com.example.knowledgecollector.application.intelligence;

import java.time.OffsetDateTime;
import java.util.List;

public record ArticleIntelligenceView(
        Long articleId, String provider, String model, String status,
        String oneSentenceSummary, List<String> keyPoints, List<String> keywords,
        List<String> tags, String category, Integer readingValue,
        Integer promptTokens, Integer responseTokens, Long durationMillis,
        String errorMessage, OffsetDateTime analyzedAt
) {
}
