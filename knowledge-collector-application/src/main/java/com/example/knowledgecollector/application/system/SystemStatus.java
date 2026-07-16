package com.example.knowledgecollector.application.system;

import java.time.OffsetDateTime;

public record SystemStatus(
        String applicationName,
        String applicationVersion,
        String databaseProduct,
        String databaseVersion,
        int flywayMigrationCount,
        long startupCount,
        OffsetDateTime lastStartedAt,
        String dataDirectory
) {
}
