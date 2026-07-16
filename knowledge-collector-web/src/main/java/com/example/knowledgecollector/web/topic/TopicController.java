package com.example.knowledgecollector.web.topic;

import com.example.knowledgecollector.application.topic.TopicService;
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
@RequestMapping("/api/v1/topics")
@Tag(name = "主题管理", description = "主题 CRUD、分页筛选、启停和受控删除")
public class TopicController {

    private final TopicService service;

    public TopicController(TopicService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "分页查询主题")
    public ApiResponse<?> list(@RequestParam(required = false) String keyword,
                               @RequestParam(required = false) Boolean enabled,
                               @RequestParam(defaultValue = "0") @Min(0) int page,
                               @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
                               HttpServletRequest request) {
        return ApiResponse.success(service.findPage(keyword, enabled, page, size), correlationId(request));
    }

    @GetMapping("/options")
    public ApiResponse<?> options(HttpServletRequest request) {
        return ApiResponse.success(service.findEnabled(), correlationId(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<?> get(@PathVariable long id, HttpServletRequest request) {
        return ApiResponse.success(service.get(id), correlationId(request));
    }

    @PostMapping
    @Operation(summary = "创建主题", description = "编码和名称必须唯一，编码保存时转为大写")
    public ResponseEntity<ApiResponse<?>> create(@Valid @RequestBody TopicRequest body,
                                                  HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(service.create(body.toCommand()), correlationId(request)));
    }

    @PutMapping("/{id}")
    public ApiResponse<?> update(@PathVariable long id, @Valid @RequestBody TopicRequest body,
                                 HttpServletRequest request) {
        return ApiResponse.success(service.update(id, body.toCommand()), correlationId(request));
    }

    @PatchMapping("/{id}/enabled")
    public ApiResponse<?> setEnabled(@PathVariable long id,
                                     @Valid @RequestBody EnabledRequest body,
                                     HttpServletRequest request) {
        return ApiResponse.success(service.setEnabled(id, body.enabled()), correlationId(request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除主题", description = "已关联采集源时返回 409")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    private String correlationId(HttpServletRequest request) {
        return (String) request.getAttribute(CorrelationIdFilter.ATTRIBUTE_NAME);
    }
}
