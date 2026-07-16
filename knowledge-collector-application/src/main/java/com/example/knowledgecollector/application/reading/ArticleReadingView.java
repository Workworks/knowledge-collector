package com.example.knowledgecollector.application.reading;

import java.time.OffsetDateTime;
import java.util.List;

public record ArticleReadingView(
        long articleId, String readingStatus, boolean favorite, boolean archived,
        String note, OffsetDateTime noteUpdatedAt, List<TagView> tags
) {
}
