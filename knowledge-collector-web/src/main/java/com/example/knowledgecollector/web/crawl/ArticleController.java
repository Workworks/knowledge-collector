package com.example.knowledgecollector.web.crawl;

import com.example.knowledgecollector.application.article.ArticleService;
import com.example.knowledgecollector.web.api.ApiResponse;
import com.example.knowledgecollector.web.api.CorrelationIdFilter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/articles")
@Tag(name = "文章查询", description = "查询 Feed 采集形成的文章元数据")
public class ArticleController {
    private final ArticleService service;

    public ArticleController(ArticleService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "分页查询文章")
    public ApiResponse<?> list(@RequestParam(required = false) String keyword,
                               @RequestParam(required = false) Long sourceId,
                               @RequestParam(defaultValue = "0") @Min(0) int page,
                               @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
                               HttpServletRequest request) {
        return ApiResponse.success(service.findPage(keyword, sourceId, page, size), correlationId(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询文章详情")
    public ApiResponse<?> get(@PathVariable long id, HttpServletRequest request) {
        return ApiResponse.success(service.get(id), correlationId(request));
    }

    private String correlationId(HttpServletRequest request) {
        return (String) request.getAttribute(CorrelationIdFilter.ATTRIBUTE_NAME);
    }
}
