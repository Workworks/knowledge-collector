package com.example.knowledgecollector.application.operations;

import com.example.knowledgecollector.application.crawl.CrawlTaskService;
import com.example.knowledgecollector.application.exception.BusinessRuleException;
import com.example.knowledgecollector.application.source.CrawlSourceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class OperationsService {
    private final OperationsGateway gateway;
    private final CrawlSourceService sources;
    private final CrawlTaskService tasks;

    public OperationsService(OperationsGateway gateway, CrawlSourceService sources,
                             CrawlTaskService tasks) {
        this.gateway = gateway;
        this.sources = sources;
        this.tasks = tasks;
    }

    @Transactional(readOnly = true)
    public DashboardView dashboard() {
        return gateway.dashboard();
    }

    @Transactional(readOnly = true)
    public List<ScheduleView> schedules() {
        return gateway.schedules();
    }

    @Transactional
    public ScheduleView saveSchedule(long sourceId, boolean enabled, int intervalMinutes) {
        sources.get(sourceId);
        if (intervalMinutes < 1 || intervalMinutes > 10080) {
            throw new BusinessRuleException("SCHEDULE-INTERVAL-INVALID",
                    "调度周期必须在 1—10080 分钟之间");
        }
        return gateway.saveSchedule(sourceId, enabled, intervalMinutes);
    }

    public void runDueSchedules() {
        OffsetDateTime now = OffsetDateTime.now();
        for (ScheduleView schedule : gateway.dueSchedules(now)) {
            tasks.runSource(schedule.sourceId(), "SCHEDULED", null);
            gateway.scheduleCompleted(schedule.sourceId(), OffsetDateTime.now(), schedule.intervalMinutes());
        }
    }

    public BackupView createBackup() {
        return gateway.createBackup();
    }

    public List<BackupView> backups() {
        return gateway.backups();
    }
}
