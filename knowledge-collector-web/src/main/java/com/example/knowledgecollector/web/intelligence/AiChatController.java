package com.example.knowledgecollector.web.intelligence;

import com.example.knowledgecollector.application.intelligence.AiChatService;
import com.example.knowledgecollector.web.api.ApiResponse;
import com.example.knowledgecollector.web.api.CorrelationIdFilter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai/chat")
@Tag(name = "AI 对话", description = "持久化 AI 对话，并将回复保存为待审核资料")
public class AiChatController {
    private final AiChatService service;

    public AiChatController(AiChatService service) {
        this.service = service;
    }

    @GetMapping("/conversations")
    @Operation(summary = "查询对话列表")
    public ApiResponse<?> list(HttpServletRequest request) {
        return ApiResponse.success(service.list(), correlationId(request));
    }

    @PostMapping("/conversations")
    @Operation(summary = "创建对话")
    public ApiResponse<?> create(@Valid @RequestBody(required = false) CreateConversationRequest body,
                                 HttpServletRequest request) {
        return ApiResponse.success(service.create(body == null ? null : body.title(),
                body == null ? null : body.provider()), correlationId(request));
    }

    @GetMapping("/conversations/{conversationId}")
    @Operation(summary = "查询对话消息")
    public ApiResponse<?> get(@PathVariable long conversationId, HttpServletRequest request) {
        return ApiResponse.success(service.get(conversationId), correlationId(request));
    }

    @PostMapping("/conversations/{conversationId}/messages")
    @Operation(summary = "发送消息并获取 AI 回复")
    public ApiResponse<?> send(@PathVariable long conversationId,
                               @Valid @RequestBody SendMessageRequest body,
                               HttpServletRequest request) {
        return ApiResponse.success(service.send(conversationId, body.content()), correlationId(request));
    }

    @PostMapping("/messages/{messageId}/save")
    @Operation(summary = "将 AI 回复保存为待审核资料")
    public ApiResponse<?> save(@PathVariable long messageId,
                               @Valid @RequestBody SaveMaterialRequest body,
                               HttpServletRequest request) {
        return ApiResponse.success(service.saveMaterial(messageId, body.title()), correlationId(request));
    }

    private String correlationId(HttpServletRequest request) {
        return (String) request.getAttribute(CorrelationIdFilter.ATTRIBUTE_NAME);
    }

    public record CreateConversationRequest(@Size(max = 200) String title,
                                            @Size(max = 64) String provider) {
    }

    public record SendMessageRequest(@NotBlank @Size(max = 8000) String content) {
    }

    public record SaveMaterialRequest(@NotBlank @Size(max = 500) String title) {
    }
}
