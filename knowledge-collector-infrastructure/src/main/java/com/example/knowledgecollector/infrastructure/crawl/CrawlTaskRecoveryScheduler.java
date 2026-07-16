package com.example.knowledgecollector.infrastructure.crawl;

import com.example.knowledgecollector.application.crawl.CrawlTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.OffsetDateTime;

@Component
public class CrawlTaskRecoveryScheduler {
    private static final Logger log = LoggerFactory.getLogger(CrawlTaskRecoveryScheduler.class);
    private final CrawlTaskService tasks;
    private final Duration staleTimeout;

    public CrawlTaskRecoveryScheduler(CrawlTaskService tasks,
            @Value("${knowledge-collector.tasks.stale-timeout:PT10M}") Duration staleTimeout) {
        this.tasks = tasks;
        this.staleTimeout = staleTimeout;
    }

    @Scheduled(initialDelayString = "${knowledge-collector.tasks.recovery-initial-delay-millis:1000}",
            fixedDelayString = "${knowledge-collector.tasks.recovery-delay-millis:60000}")
    public void recover() {
        int recovered = tasks.recoverStaleTasks(OffsetDateTime.now().minus(staleTimeout));
        if (recovered > 0) {
            log.warn("Recovered {} stale crawl task(s)", recovered);
        }
    }
}
