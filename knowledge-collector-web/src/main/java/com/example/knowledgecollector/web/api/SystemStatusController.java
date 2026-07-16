package com.example.knowledgecollector.web.api;

import com.example.knowledgecollector.application.system.SystemStatus;
import com.example.knowledgecollector.application.system.SystemStatusQuery;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/system")
public class SystemStatusController {

    private final SystemStatusQuery statusQuery;

    public SystemStatusController(SystemStatusQuery statusQuery) {
        this.statusQuery = statusQuery;
    }

    @GetMapping("/status")
    public ApiResponse<SystemStatus> status(HttpServletRequest request) {
        return ApiResponse.success(
                statusQuery.getStatus(),
                (String) request.getAttribute(CorrelationIdFilter.ATTRIBUTE_NAME)
        );
    }
}
