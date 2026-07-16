package com.example.knowledgecollector.web.intelligence;

import com.example.knowledgecollector.application.intelligence.ArticleIntelligenceService;
import com.example.knowledgecollector.web.api.ApiResponse;
import com.example.knowledgecollector.web.api.CorrelationIdFilter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "AI 内容理解", description = "可替换 Provider 的文章摘要、要点、关键词与标签分析")
public class ArticleIntelligenceController {
    private final ArticleIntelligenceService service;

    public ArticleIntelligenceController(ArticleIntelligenceService service) {
        this.service = service;
    }

    @GetMapping("/ai/providers")
    @Operation(summary = "查询 AI Provider 状态")
    public ApiResponse<?> providers(HttpServletRequest request) {
        return ApiResponse.success(service.providers(), correlationId(request));
    }

    @GetMapping("/articles/{articleId}/ai")
    @Operation(summary = "查询文章最近一次 AI 分析")
    public ApiResponse<?> get(@PathVariable long articleId, HttpServletRequest request) {
        return ApiResponse.success(service.get(articleId), correlationId(request));
    }

    @PostMapping("/articles/{articleId}/ai/analyze")
    @Operation(summary = "使用指定或系统默认 Provider 分析文章")
    public ApiResponse<?> analyze(@PathVariable long articleId,
                                  @RequestParam(required = false) String provider,
                                  HttpServletRequest request) {
        return ApiResponse.success(service.analyze(articleId, provider), correlationId(request));
    }

    private String correlationId(HttpServletRequest request) {
        return (String) request.getAttribute(CorrelationIdFilter.ATTRIBUTE_NAME);
    }
}
