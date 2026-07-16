package com.example.knowledgecollector.web.source;

import com.example.knowledgecollector.application.source.CrawlSourceService;
import com.example.knowledgecollector.domain.source.SourceType;
import com.example.knowledgecollector.web.api.ApiResponse;
import com.example.knowledgecollector.web.api.CorrelationIdFilter;
import com.example.knowledgecollector.web.api.EnabledRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/sources")
@Tag(name = "采集源管理", description = "采集源 CRUD、分页筛选、主题关系和启停")
public class CrawlSourceController {

    private final CrawlSourceService service;

    public CrawlSourceController(CrawlSourceService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "分页查询采集源")
    public ApiResponse<?> list(@RequestParam(required = false) String keyword,
                               @RequestParam(required = false) SourceType type,
                               @RequestParam(required = false) Boolean enabled,
                               @RequestParam(required = false) Long topicId,
                               @RequestParam(defaultValue = "0") @Min(0) int page,
                               @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
                               HttpServletRequest request) {
        return ApiResponse.success(service.findPage(keyword, type, enabled, topicId, page, size),
                correlationId(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<?> get(@PathVariable long id, HttpServletRequest request) {
        return ApiResponse.success(service.get(id), correlationId(request));
    }

    @PostMapping
    @Operation(summary = "创建采集源", description = "URL 只允许 HTTP/HTTPS，可关联多个主题")
    public ResponseEntity<ApiResponse<?>> create(@Valid @RequestBody CrawlSourceRequest body,
                                                  HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(service.create(body.toCommand()), correlationId(request)));
    }

    @PutMapping("/{id}")
    public ApiResponse<?> update(@PathVariable long id,
                                 @Valid @RequestBody CrawlSourceRequest body,
                                 HttpServletRequest request) {
        return ApiResponse.success(service.update(id, body.toCommand()), correlationId(request));
    }

    @PatchMapping("/{id}/enabled")
    public ApiResponse<?> setEnabled(@PathVariable long id, @Valid @RequestBody EnabledRequest body,
                                     HttpServletRequest request) {
        return ApiResponse.success(service.setEnabled(id, body.enabled()), correlationId(request));
    }

    @PostMapping("/{id}/test")
    @Operation(summary = "测试采集源", description = "Stage 4 返回 422；Stage 5 接入 Provider")
    public ApiResponse<?> test(@PathVariable long id, HttpServletRequest request) {
        service.assertTestingAvailable(id);
        return ApiResponse.success(null, correlationId(request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    private String correlationId(HttpServletRequest request) {
        return (String) request.getAttribute(CorrelationIdFilter.ATTRIBUTE_NAME);
    }
}
