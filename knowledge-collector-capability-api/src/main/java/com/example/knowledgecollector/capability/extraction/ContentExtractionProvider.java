package com.example.knowledgecollector.capability.extraction;

import java.time.OffsetDateTime;
import java.util.Map;

public interface ContentExtractionProvider {
    String id();
    ExtractionResult extract(ExtractionRequest request);

    record ExtractionRequest(String url, int timeoutSeconds, Map<String, String> options) {}
    record ExtractionResult(String finalUrl, String title, String author, OffsetDateTime publishedAt,
                            String contentHtml, String contentText, String rawHtml,
                            byte[] screenshot, Map<String, String> metadata) {}
}
