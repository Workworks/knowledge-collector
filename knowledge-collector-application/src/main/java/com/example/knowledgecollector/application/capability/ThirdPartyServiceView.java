package com.example.knowledgecollector.application.capability;

import java.time.OffsetDateTime;
import java.util.List;

public record ThirdPartyServiceView(long id, String providerId, String serviceName, String serviceType,
        String implementationName, String endpoint, String model, String authenticationType,
        boolean credentialConfigured, boolean enabled, boolean defaultProvider, boolean fallbackProvider, boolean userConfigured,
        String healthStatus, OffsetDateTime lastCheckedAt, OffsetDateTime lastSuccessAt, String lastError,
        long todayCalls, long averageDurationMillis, List<String> businessUsages) {
}
