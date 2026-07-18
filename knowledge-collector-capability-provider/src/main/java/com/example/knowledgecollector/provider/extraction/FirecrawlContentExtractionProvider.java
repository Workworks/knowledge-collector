package com.example.knowledgecollector.provider.extraction;

import com.example.knowledgecollector.capability.extraction.ContentExtractionProvider;
import com.example.knowledgecollector.capability.management.ManagedCapabilityProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Component
public class FirecrawlContentExtractionProvider implements ContentExtractionProvider, ManagedCapabilityProvider {
    private final ObjectMapper json;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
    private volatile RuntimeConfiguration configuration;

    public FirecrawlContentExtractionProvider(ObjectMapper json,
            @Value("${knowledge-collector.extraction.firecrawl.enabled:false}") boolean enabled,
            @Value("${knowledge-collector.extraction.firecrawl.base-url:http://127.0.0.1:3002}") String endpoint,
            @Value("${knowledge-collector.extraction.firecrawl.api-key:}") String apiKey) {
        this.json = json;
        this.configuration = new RuntimeConfiguration(enabled, normalize(endpoint), null,
                apiKey.isBlank() ? "NONE" : "BEARER", apiKey);
    }

    @Override public String id() { return "firecrawl"; }
    @Override public String serviceType() { return "EXTRACTION"; }
    @Override public String displayName() { return "Firecrawl 正文提取"; }
    @Override public String implementationName() { return getClass().getName(); }
    @Override public List<String> businessUsages() { return List.of("文章正文重新提取", "手动 URL 提取", "原始网页证据保存"); }
    @Override public RuntimeConfiguration currentConfiguration() { return configuration; }
    @Override public synchronized void configure(RuntimeConfiguration value) {
        configuration = new RuntimeConfiguration(value.enabled(), normalize(value.endpoint()), value.model(),
                value.authenticationType(), value.credential());
    }

    @Override public ConnectionResult testConnection() {
        if (!configuration.enabled()) return new ConnectionResult(false, "Firecrawl 已停用", List.of());
        try {
            var response = http.send(HttpRequest.newBuilder(URI.create(configuration.endpoint()))
                    .timeout(Duration.ofSeconds(8)).GET().build(), HttpResponse.BodyHandlers.discarding());
            boolean ok = response.statusCode() < 500;
            return new ConnectionResult(ok, ok ? "Firecrawl 服务可访问" : "Firecrawl 返回 HTTP " + response.statusCode(), List.of());
        } catch (Exception e) { return new ConnectionResult(false, "无法连接 Firecrawl：" + safe(e), List.of()); }
    }

    @Override public ExtractionResult extract(ExtractionRequest request) {
        ensureEnabled();
        try {
            String body = json.writeValueAsString(Map.of("url", request.url(), "formats", List.of("html", "markdown"),
                    "onlyMainContent", true, "waitFor", 1000));
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(configuration.endpoint() + "/v1/scrape"))
                    .timeout(Duration.ofSeconds(Math.max(5, request.timeoutSeconds())))
                    .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body));
            if (configuration.credential() != null && !configuration.credential().isBlank())
                builder.header("Authorization", "Bearer " + configuration.credential());
            HttpResponse<String> response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() / 100 != 2) throw new IllegalStateException("FIRECRAWL-HTTP-" + response.statusCode() + ": " + response.body());
            JsonNode data = json.readTree(response.body()).path("data");
            String raw = text(data, "html");
            String markdown = text(data, "markdown");
            JsonNode meta = data.path("metadata");
            String cleanHtml = raw == null ? null : Jsoup.clean(raw, request.url(), Safelist.relaxed());
            String cleanText = cleanHtml == null ? markdown : Jsoup.parse(cleanHtml).text();
            return new ExtractionResult(text(meta, "sourceURL") == null ? request.url() : text(meta, "sourceURL"),
                    text(meta, "title"), text(meta, "author"), date(text(meta, "publishedTime")),
                    cleanHtml, cleanText, raw, null, Map.of("provider", id()));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); throw new IllegalStateException("FIRECRAWL-INTERRUPTED", e);
        } catch (Exception e) { throw new IllegalStateException("FIRECRAWL-EXTRACTION-FAILED: " + safe(e), e); }
    }

    private void ensureEnabled() { if (!configuration.enabled()) throw new IllegalStateException("FIRECRAWL-DISABLED: Firecrawl 已停用"); }
    private String normalize(String value) {
        URI uri = URI.create(value.endsWith("/") ? value.substring(0, value.length() - 1) : value);
        if (!List.of("http", "https").contains(uri.getScheme())) throw new IllegalArgumentException("Firecrawl 地址只允许 HTTP/HTTPS");
        return uri.toString();
    }
    private String text(JsonNode node, String name) { String value = node.path(name).asText(null); return value == null || value.isBlank() ? null : value; }
    private OffsetDateTime date(String value) { try { return value == null ? null : OffsetDateTime.parse(value); } catch (Exception ignored) { return null; } }
    private String safe(Exception e) { return e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage(); }
}
