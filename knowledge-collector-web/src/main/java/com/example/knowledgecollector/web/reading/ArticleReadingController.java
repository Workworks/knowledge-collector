package com.example.knowledgecollector.web.reading;

import com.example.knowledgecollector.application.reading.ArticleReadingService;
import com.example.knowledgecollector.web.api.ApiResponse;
import com.example.knowledgecollector.web.api.CorrelationIdFilter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/articles/{articleId}/reading")
@Tag(name = "阅读管理", description = "阅读状态、收藏、归档、忽略、标签和笔记")
public class ArticleReadingController {
    private final ArticleReadingService service;

    public ArticleReadingController(ArticleReadingService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "查询文章阅读信息")
    public ApiResponse<?> get(@PathVariable long articleId, HttpServletRequest request) {
        return ApiResponse.success(service.get(articleId), correlationId(request));
    }

    @PatchMapping("/state")
    @Operation(summary = "更新阅读状态、收藏或归档")
    public ApiResponse<?> updateState(@PathVariable long articleId,
                                      @Valid @RequestBody StateRequest body,
                                      HttpServletRequest request) {
        return ApiResponse.success(service.updateState(articleId, body.readingStatus(),
                body.favorite(), body.archived()), correlationId(request));
    }

    @PutMapping("/note")
    @Operation(summary = "保存个人笔记")
    public ApiResponse<?> saveNote(@PathVariable long articleId,
                                   @Valid @RequestBody NoteRequest body,
                                   HttpServletRequest request) {
        return ApiResponse.success(service.saveNote(articleId, body.content()), correlationId(request));
    }

    @PutMapping("/tags")
    @Operation(summary = "替换文章自定义标签")
    public ApiResponse<?> replaceTags(@PathVariable long articleId,
                                      @Valid @RequestBody TagsRequest body,
                                      HttpServletRequest request) {
        return ApiResponse.success(service.replaceTags(articleId, body.tagNames()), correlationId(request));
    }

    public record StateRequest(String readingStatus, Boolean favorite, Boolean archived) {
    }

    public record NoteRequest(@Size(max = 10000) String content) {
    }

    public record TagsRequest(@Size(max = 2000) String tagNames) {
    }

    private String correlationId(HttpServletRequest request) {
        return (String) request.getAttribute(CorrelationIdFilter.ATTRIBUTE_NAME);
    }
}
