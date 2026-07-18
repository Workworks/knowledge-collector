package com.example.knowledgecollector.provider.search;

import com.example.knowledgecollector.capability.management.ManagedCapabilityProvider;
import com.example.knowledgecollector.capability.source.SourceDiscoveryProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
public class SearxngSourceDiscoveryProvider implements SourceDiscoveryProvider, ManagedCapabilityProvider {
    private final ObjectMapper json;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private volatile boolean enabled;
    private volatile URI endpoint;

    public SearxngSourceDiscoveryProvider(ObjectMapper json,
            @Value("${knowledge-collector.search.searxng.enabled:false}") boolean enabled,
            @Value("${knowledge-collector.search.searxng.base-url:http://127.0.0.1:8088}") String endpoint) {
        this.json = json;
        this.enabled = enabled;
        this.endpoint = endpoint(endpoint);
    }

    @Override public String id() { return "searxng"; }
    @Override public String serviceType() { return "SEARCH"; }
    @Override public String displayName() { return "SearXNG 元搜索"; }
    @Override public String implementationName() { return getClass().getName(); }
    @Override public List<String> businessUsages() { return List.of("自动发现采集源", "公开网站检索"); }
    @Override public RuntimeConfiguration currentConfiguration() { return new RuntimeConfiguration(enabled, endpoint.toString(), null, "NONE", null); }

    @Override
    public synchronized void configure(RuntimeConfiguration configuration) {
        enabled = configuration.enabled();
        endpoint = endpoint(configuration.endpoint());
    }

    @Override
    public ConnectionResult testConnection() {
        if (!enabled) return new ConnectionResult(false, "SearXNG 已停用", List.of());
        try {
            HttpResponse<String> response = send("Knowledge Collector", "all", 1);
            boolean ok = response.statusCode() == 200 && json.readTree(response.body()).path("results").isArray();
            return new ConnectionResult(ok, ok ? "SearXNG 连接成功" : "SearXNG 返回 HTTP " + response.statusCode(), List.of());
        } catch (Exception exception) {
            return new ConnectionResult(false, "无法连接 SearXNG：" + safe(exception), List.of());
        }
    }

    @Override
    public List<Candidate> discover(Request request) {
        if (!enabled) throw new IllegalStateException("SEARXNG-DISABLED: SearXNG 已停用");
        String type = normalizeType(request.sourceType());
        String suffix = switch (type) {
            case "RSS" -> " RSS feed";
            case "ATOM" -> " Atom feed";
            case "JSON_API" -> " public API";
            default -> " official news research";
        };
        try {
            HttpResponse<String> response = send(request.topic().trim() + suffix, request.language(), request.count() * 3);
            if (response.statusCode() != 200) {
                throw new IllegalStateException("SEARXNG-HTTP-" + response.statusCode());
            }
            Set<String> seen = new LinkedHashSet<>();
            List<Candidate> candidates = new ArrayList<>();
            for (JsonNode item : json.readTree(response.body()).path("results")) {
                String url = item.path("url").asText();
                if (!url.matches("https?://.+") || !seen.add(url.toLowerCase(Locale.ROOT))) continue;
                URI uri = URI.create(url);
                String home = uri.getScheme() + "://" + uri.getAuthority();
                String title = item.path("title").asText(uri.getHost());
                int score = reliability(uri, item.path("content").asText(), request.qualityLevel());
                candidates.add(new Candidate(title, home, url, type, normalizeLanguage(request.language()), score,
                        "SearXNG 检索命中；" + (score >= 80 ? "域名与主题匹配度高" : "建议验证页面结构与更新频率")));
                if (candidates.size() >= Math.max(1, Math.min(request.count(), 50))) break;
            }
            return List.copyOf(candidates);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("SEARXNG-INTERRUPTED: 搜索被中断", exception);
        } catch (IllegalStateException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("SEARXNG-REQUEST-FAILED: " + safe(exception), exception);
        }
    }

    private HttpResponse<String> send(String query, String language, int count) throws Exception {
        String url = endpoint + "/search?q=" + URLEncoder.encode(query, StandardCharsets.UTF_8)
                + "&format=json&language=" + URLEncoder.encode(normalizeLanguage(language), StandardCharsets.UTF_8)
                + "&pageno=1&categories=general";
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(15))
                .header("Accept", "application/json").GET().build();
        return http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private int reliability(URI uri, String description, String quality) {
        int score = "https".equalsIgnoreCase(uri.getScheme()) ? 68 : 55;
        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
        if (host.endsWith(".gov") || host.contains(".gov.") || host.endsWith(".edu") || host.contains(".edu.")) score += 20;
        if ((description == null ? "" : description).length() > 80) score += 5;
        if ("权威".equals(quality)) score -= host.contains("gov") || host.contains("edu") ? 0 : 8;
        return Math.max(0, Math.min(100, score));
    }

    private URI endpoint(String value) {
        URI uri = URI.create(value.endsWith("/") ? value.substring(0, value.length() - 1) : value);
        if (!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme()))
            throw new IllegalArgumentException("SearXNG 地址只允许 HTTP/HTTPS");
        return uri;
    }

    private String normalizeType(String value) {
        if (value == null || value.isBlank() || "ALL".equalsIgnoreCase(value)) return "HTML_LIST";
        String normalized = value.toUpperCase(Locale.ROOT);
        return Set.of("RSS", "ATOM", "HTML_LIST", "JSON_API", "MANUAL_URL").contains(normalized) ? normalized : "HTML_LIST";
    }
    private String normalizeLanguage(String value) { return value != null && value.toLowerCase(Locale.ROOT).startsWith("en") ? "en" : "zh-CN"; }
    private String safe(Exception e) { return e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage(); }
}
