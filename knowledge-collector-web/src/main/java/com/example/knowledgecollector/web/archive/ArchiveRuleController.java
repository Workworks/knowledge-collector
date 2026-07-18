package com.example.knowledgecollector.web.archive;

import com.example.knowledgecollector.application.archive.ArchiveRuleService;
import com.example.knowledgecollector.web.api.ApiResponse;
import com.example.knowledgecollector.web.api.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/archive-rules")
public class ArchiveRuleController {
    private final ArchiveRuleService service;
    public ArchiveRuleController(ArchiveRuleService service) { this.service = service; }

    @GetMapping
    public ApiResponse<?> list(HttpServletRequest request) {
        return ApiResponse.success(service.findAll(), cid(request));
    }

    @PostMapping
    public ApiResponse<?> create(@Valid @RequestBody RuleRequest body, HttpServletRequest request) {
        return ApiResponse.success(service.save(null, body.name(), body.keyword(), body.topicId(), body.sourceId(),
                body.minQuality(), body.sortOrder(), body.enabled()), cid(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<?> update(@PathVariable long id, @Valid @RequestBody RuleRequest body,
                                 HttpServletRequest request) {
        return ApiResponse.success(service.save(id, body.name(), body.keyword(), body.topicId(), body.sourceId(),
                body.minQuality(), body.sortOrder(), body.enabled()), cid(request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        service.delete(id); return ResponseEntity.noContent().build();
    }

    private String cid(HttpServletRequest request) {
        return (String) request.getAttribute(CorrelationIdFilter.ATTRIBUTE_NAME);
    }

    public record RuleRequest(@NotBlank @Size(max=128) String name, @Size(max=200) String keyword,
                              Long topicId, Long sourceId, @Min(0) @Max(100) Integer minQuality,
                              int sortOrder, boolean enabled) {}
}
