package com.example.knowledgecollector.application.operations;

import java.time.OffsetDateTime;

public record ScheduleView(long sourceId, String sourceName, boolean enabled, int intervalMinutes,
                           OffsetDateTime nextRunAt, OffsetDateTime lastRunAt) {
}
