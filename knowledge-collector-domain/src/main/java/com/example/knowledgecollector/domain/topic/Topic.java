package com.example.knowledgecollector.domain.topic;

import java.time.OffsetDateTime;
import java.util.List;

public record Topic(
        Long id,
        String code,
        String name,
        String description,
        List<String> keywords,
        List<String> excludedKeywords,
        String color,
        String icon,
        String language,
        boolean enabled,
        int sortOrder,
        long version,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
