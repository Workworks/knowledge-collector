package com.example.knowledgecollector.web.rule;

import com.example.knowledgecollector.application.crawl.CrawlTaskService;
import com.example.knowledgecollector.application.rule.SourceRuleService;
import com.example.knowledgecollector.web.api.ApiResponse;
import com.example.knowledgecollector.web.api.CorrelationIdFilter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/sources/{sourceId}/rules")
@Tag(name = "HTML 采集规则", description = "管理 CSS 选择器规则版本并执行无入库预览")
public class SourceRuleController {
    private final SourceRuleService rules;
    private final CrawlTaskService crawlTasks;

    public SourceRuleController(SourceRuleService rules, CrawlTaskService crawlTasks) {
        this.rules = rules;
        this.crawlTasks = crawlTasks;
    }

    @GetMapping
    @Operation(summary = "查询规则版本")
    public ApiResponse<?> versions(@PathVariable long sourceId, HttpServletRequest request) {
        return ApiResponse.success(rules.versions(sourceId), correlationId(request));
    }

    @GetMapping("/active")
    @Operation(summary = "查询当前启用规则")
    public ApiResponse<?> active(@PathVariable long sourceId, HttpServletRequest request) {
        return ApiResponse.success(rules.active(sourceId), correlationId(request));
    }

    @PostMapping
    @Operation(summary = "创建新规则版本")
    public ResponseEntity<ApiResponse<?>> create(@PathVariable long sourceId,
                                                  @Valid @RequestBody SourceRuleRequest body,
                                                  HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(rules.create(sourceId, body.toCommand()), correlationId(request)));
    }

    @PutMapping("/{ruleId}/active")
    @Operation(summary = "激活指定规则版本")
    public ApiResponse<?> activate(@PathVariable long sourceId, @PathVariable long ruleId,
                                   HttpServletRequest request) {
        return ApiResponse.success(rules.activate(sourceId, ruleId), correlationId(request));
    }

    @PostMapping("/test")
    @Operation(summary = "测试当前规则", description = "请求列表与详情页面并返回解析数量，不写入文章")
    public ApiResponse<?> test(@PathVariable long sourceId, HttpServletRequest request) {
        return ApiResponse.success(Map.of("entryCount", crawlTasks.testSource(sourceId)),
                correlationId(request));
    }

    private String correlationId(HttpServletRequest request) {
        return (String) request.getAttribute(CorrelationIdFilter.ATTRIBUTE_NAME);
    }
}
