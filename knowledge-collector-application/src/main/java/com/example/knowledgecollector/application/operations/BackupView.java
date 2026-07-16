package com.example.knowledgecollector.application.operations;

import java.time.OffsetDateTime;

public record BackupView(long id, String name, String relativePath, long sizeBytes,
                         String status, OffsetDateTime createdAt) {
}
