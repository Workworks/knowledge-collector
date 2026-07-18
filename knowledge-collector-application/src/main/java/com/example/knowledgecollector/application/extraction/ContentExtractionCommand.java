package com.example.knowledgecollector.application.extraction;
public record ContentExtractionCommand(Long articleId, String url, String method, Long retryOfId) {}
