package com.example.knowledgecollector.web.page;

import com.example.knowledgecollector.application.demo.DemoDataService;
import com.example.knowledgecollector.application.system.SystemStatusQuery;
import com.example.knowledgecollector.web.api.ApiResponse;
import com.example.knowledgecollector.web.api.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Arrays;

@Controller
@Profile("local")
public class DevToolsController {

    private final DemoDataService demoDataService;
    private final SystemStatusQuery statusQuery;
    private final Environment environment;

    public DevToolsController(DemoDataService demoDataService, SystemStatusQuery statusQuery,
                              Environment environment) {
        this.demoDataService = demoDataService;
        this.statusQuery = statusQuery;
        this.environment = environment;
    }

    @GetMapping("/dev/tools")
    public String tools(Model model) {
        model.addAttribute("profiles", Arrays.asList(environment.getActiveProfiles()));
        model.addAttribute("status", statusQuery.getStatus());
        return "dev-tools";
    }

    @PostMapping("/api/v1/dev/demo/initialize")
    @ResponseBody
    public ApiResponse<String> initialize(HttpServletRequest request) {
        demoDataService.initialize();
        return ApiResponse.success("演示主题与来源已初始化", correlationId(request));
    }

    @PostMapping("/api/v1/dev/demo/clear")
    @ResponseBody
    public ApiResponse<String> clear(HttpServletRequest request) {
        demoDataService.clear();
        return ApiResponse.success("演示主题与来源已清空", correlationId(request));
    }

    private String correlationId(HttpServletRequest request) {
        return (String) request.getAttribute(CorrelationIdFilter.ATTRIBUTE_NAME);
    }
}
