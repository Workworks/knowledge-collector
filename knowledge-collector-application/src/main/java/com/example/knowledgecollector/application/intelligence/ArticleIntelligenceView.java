package com.example.knowledgecollector.application.intelligence;

import java.time.OffsetDateTime;
import java.util.List;

public record ArticleIntelligenceView(
        Long articleId, String provider, String model, String status,
        String oneSentenceSummary, String coreSummary, List<String> outline,
        List<String> keyPoints, List<String> keyConclusions, List<String> keyData,
        List<String> importantCases, List<String> people, List<String> organizations,
        List<String> products, List<String> technologies, List<String> locations,
        List<String> timeInformation, List<String> keywords, List<String> tags,
        String category, String articleType, Integer readingValue, Integer qualityScore,
        String sourceCredibility, String readingReason, List<String> informationNature,
        Integer promptTokens, Integer responseTokens, Long durationMillis,
        String errorMessage, OffsetDateTime analyzedAt
) {
}
