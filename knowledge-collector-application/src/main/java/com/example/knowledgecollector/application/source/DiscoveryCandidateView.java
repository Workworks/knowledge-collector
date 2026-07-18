package com.example.knowledgecollector.application.source;

import java.time.OffsetDateTime;

public record DiscoveryCandidateView(long id, String providerId, String topic, String name,
        String websiteUrl, String collectionUrl, String sourceType, String language,
        int reliabilityScore, String validationStatus, String recommendationReason,
        String validationMessage, Long importedSourceId, boolean ignored,
        OffsetDateTime createdAt, OffsetDateTime validatedAt) {
}
