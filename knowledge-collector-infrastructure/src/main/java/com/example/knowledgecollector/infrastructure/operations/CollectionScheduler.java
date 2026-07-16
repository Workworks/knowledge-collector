package com.example.knowledgecollector.infrastructure.operations;

import com.example.knowledgecollector.application.operations.OperationsService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CollectionScheduler {
    private final OperationsService operations;

    public CollectionScheduler(OperationsService operations) {
        this.operations = operations;
    }

    @Scheduled(fixedDelayString = "${knowledge-collector.scheduling.scan-delay-millis:30000}")
    public void runDueSchedules() {
        operations.runDueSchedules();
    }
}
