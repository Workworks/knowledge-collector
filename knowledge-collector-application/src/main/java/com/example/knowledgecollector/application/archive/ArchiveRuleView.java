package com.example.knowledgecollector.application.archive;

import java.time.OffsetDateTime;

public record ArchiveRuleView(Long id, String name, String keyword, Long topicId,
                              Long sourceId, Integer minQuality, int sortOrder,
                              boolean enabled, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
}
