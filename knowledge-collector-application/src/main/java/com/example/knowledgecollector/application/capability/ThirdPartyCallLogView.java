package com.example.knowledgecollector.application.capability;

import java.time.OffsetDateTime;

public record ThirdPartyCallLogView(long id, String providerId, String operation, String businessScene,
        String status, String requestSummary, String resultSummary, String errorMessage, Long retryOfId,
        long durationMillis, OffsetDateTime createdAt, OffsetDateTime finishedAt) {
}
