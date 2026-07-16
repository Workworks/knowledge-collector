package com.example.knowledgecollector.web.operations;

import com.example.knowledgecollector.application.operations.OperationsService;
import com.example.knowledgecollector.web.api.ApiResponse;
import com.example.knowledgecollector.web.api.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Value;
import java.time.Duration;

@Validated
@RestController
@RequestMapping("/api/v1")
public class OperationsController {
    private final OperationsService service;
    private final Duration staleTimeout;

    public OperationsController(OperationsService service,
            @Value("${knowledge-collector.tasks.stale-timeout:PT10M}") Duration staleTimeout) {
        this.service = service;
        this.staleTimeout = staleTimeout;
    }

    @GetMapping("/dashboard")
    public ApiResponse<?> dashboard(HttpServletRequest request) {
        return ApiResponse.success(service.dashboard(), correlationId(request));
    }

    @GetMapping("/operations/schedules")
    public ApiResponse<?> schedules(HttpServletRequest request) {
        return ApiResponse.success(service.schedules(), correlationId(request));
    }

    @PutMapping("/operations/schedules/{sourceId}")
    public ApiResponse<?> schedule(@PathVariable long sourceId,
                                   @Valid @RequestBody ScheduleRequest body,
                                   HttpServletRequest request) {
        return ApiResponse.success(service.saveSchedule(sourceId, body.enabled(),
                body.intervalMinutes()), correlationId(request));
    }

    @PostMapping("/operations/schedules/run-due")
    public ApiResponse<?> runDue(HttpServletRequest request) {
        service.runDueSchedules();
        return ApiResponse.success("到期调度已执行", correlationId(request));
    }

    @GetMapping("/operations/backups")
    public ApiResponse<?> backups(HttpServletRequest request) {
        return ApiResponse.success(service.backups(), correlationId(request));
    }

    @PostMapping("/operations/backups")
    public ApiResponse<?> createBackup(HttpServletRequest request) {
        return ApiResponse.success(service.createBackup(), correlationId(request));
    }

    @PostMapping("/operations/tasks/recover-stale")
    public ApiResponse<?> recoverStaleTasks(HttpServletRequest request) {
        return ApiResponse.success(service.recoverStaleTasks(staleTimeout), correlationId(request));
    }

    public record ScheduleRequest(boolean enabled,
                                  @Min(1) @Max(10080) int intervalMinutes) {
    }

    private String correlationId(HttpServletRequest request) {
        return (String) request.getAttribute(CorrelationIdFilter.ATTRIBUTE_NAME);
    }
}
