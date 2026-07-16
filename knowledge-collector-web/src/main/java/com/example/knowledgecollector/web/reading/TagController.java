package com.example.knowledgecollector.web.reading;

import com.example.knowledgecollector.application.reading.ArticleReadingService;
import com.example.knowledgecollector.web.api.ApiResponse;
import com.example.knowledgecollector.web.api.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tags")
public class TagController {
    private final ArticleReadingService service;

    public TagController(ArticleReadingService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<?> list(HttpServletRequest request) {
        return ApiResponse.success(service.tags(),
                (String) request.getAttribute(CorrelationIdFilter.ATTRIBUTE_NAME));
    }
}
