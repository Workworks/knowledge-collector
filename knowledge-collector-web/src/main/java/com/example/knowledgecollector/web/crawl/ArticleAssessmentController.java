package com.example.knowledgecollector.web.crawl;

import com.example.knowledgecollector.application.assessment.ArticleAssessmentService;
import com.example.knowledgecollector.web.api.ApiResponse;
import com.example.knowledgecollector.web.api.CorrelationIdFilter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/articles/{articleId}/assessment")
@Tag(name = "文章质量控制", description = "主题匹配、排除词、质量评分、来源评级和待审核状态")
public class ArticleAssessmentController {
    private final ArticleAssessmentService service;

    public ArticleAssessmentController(ArticleAssessmentService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "查询文章评估结果")
    public ApiResponse<?> get(@PathVariable long articleId, HttpServletRequest request) {
        return ApiResponse.success(service.get(articleId), correlationId(request));
    }

    @PostMapping
    @Operation(summary = "重新评估文章")
    public ApiResponse<?> reassess(@PathVariable long articleId, HttpServletRequest request) {
        return ApiResponse.success(service.assess(articleId), correlationId(request));
    }

    private String correlationId(HttpServletRequest request) {
        return (String) request.getAttribute(CorrelationIdFilter.ATTRIBUTE_NAME);
    }
}
