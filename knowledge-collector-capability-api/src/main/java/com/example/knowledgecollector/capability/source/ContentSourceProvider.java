package com.example.knowledgecollector.capability.source;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public interface ContentSourceProvider {
    boolean supports(String sourceType);

    FetchResult fetch(FetchRequest request);

    record FetchRequest(
            String sourceType, String homeUrl, String entryUrl, String language, String charset,
            String userAgent, int timeoutSeconds, Map<String, String> options
    ) {
    }

    record FetchResult(List<ContentItem> items, Map<String, String> metadata) {
    }

    record ContentItem(
            String title, String url, String author, String summary, OffsetDateTime publishedAt,
            String contentHtml, String contentText, Map<String, String> metadata
    ) {
    }
}
