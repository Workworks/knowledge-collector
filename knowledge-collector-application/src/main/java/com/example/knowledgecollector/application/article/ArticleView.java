package com.example.knowledgecollector.application.article;

import java.time.OffsetDateTime;

public record ArticleView(
        Long id, Long sourceId, String sourceName, String title, String author, String summary,
        String originalUrl, String normalizedUrl, String language, OffsetDateTime publishTime,
        boolean publishTimeInferred, String contentHtml, String contentText,
        int wordCount, int readingMinutes,
        OffsetDateTime firstCollectedAt, OffsetDateTime lastCollectedAt
) {
}
