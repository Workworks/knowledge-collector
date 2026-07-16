package com.example.knowledgecollector.application.operations;

import java.time.OffsetDateTime;
import java.util.List;

public interface OperationsGateway {
    DashboardView dashboard();
    List<ScheduleView> schedules();
    ScheduleView saveSchedule(long sourceId, boolean enabled, int intervalMinutes);
    List<ScheduleView> dueSchedules(OffsetDateTime now);
    void scheduleCompleted(long sourceId, OffsetDateTime completedAt, int intervalMinutes);
    BackupView createBackup();
    List<BackupView> backups();
}
