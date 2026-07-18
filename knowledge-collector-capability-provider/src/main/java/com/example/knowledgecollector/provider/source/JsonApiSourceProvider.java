package com.example.knowledgecollector.provider.source;

import com.example.knowledgecollector.capability.source.ContentSourceProvider;
import com.example.knowledgecollector.capability.web.WebContentProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.core.annotation.Order;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@Order(100)
public class JsonApiSourceProvider implements ContentSourceProvider {
    private static final int MAX_BYTES = 5 * 1024 * 1024;
    private final WebContentProvider web;
    private final ObjectMapper json;

    public JsonApiSourceProvider(WebContentProvider web, ObjectMapper json) {
        this.web = web;
        this.json = json;
    }

    @Override
    public boolean supports(String sourceType) { return "JSON_API".equals(sourceType); }

    @Override
    public FetchResult fetch(FetchRequest request) {
        try {
            var response = web.get(new WebContentProvider.WebRequest(request.entryUrl(), request.userAgent(),
                    request.language(), request.timeoutSeconds(), MAX_BYTES));
            JsonNode root = json.readTree(response.body());
            JsonNode array = root.isArray() ? root : firstArray(root, "items", "articles", "results", "data");
            if (array == null || !array.isArray()) throw new IllegalStateException("JSON 响应未包含可识别的资料数组");
            List<ContentItem> items = new ArrayList<>();
            for (JsonNode node : array) {
                if (items.size() >= 50) break;
                String title = text(node, "title", "name", "headline");
                String url = text(node, "url", "link", "originalUrl");
                if (title == null || url == null || !url.matches("https?://.+")) continue;
                String content = text(node, "content", "body", "text", "description");
                String summary = text(node, "summary", "abstract", "description");
                items.add(new ContentItem(title, url, text(node, "author", "creator"), summary,
                        time(text(node, "publishedAt", "publishTime", "date")), null, content,
                        Map.of("provider", "generic-json")));
            }
            if (items.isEmpty()) throw new IllegalStateException("JSON 响应中未识别到有效资料");
            return new FetchResult(List.copyOf(items), Map.of("provider", "generic-json"));
        } catch (IllegalStateException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("JSON-API-PARSE-FAILED: " + exception.getMessage(), exception);
        }
    }

    private JsonNode firstArray(JsonNode root, String... names) {
        for (String name : names) if (root.path(name).isArray()) return root.path(name);
        return null;
    }

    private String text(JsonNode node, String... names) {
        for (String name : names) if (node.hasNonNull(name) && !node.path(name).asText().isBlank()) return node.path(name).asText();
        return null;
    }

    private OffsetDateTime time(String value) {
        if (value == null) return null;
        try { return OffsetDateTime.parse(value); } catch (Exception ignored) { return null; }
    }
}
