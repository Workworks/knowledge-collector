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
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Component
public class PlaywrightContentExtractionProvider implements ContentExtractionProvider, ManagedCapabilityProvider {
    private final ObjectMapper json;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
    private volatile RuntimeConfiguration configuration;

    public PlaywrightContentExtractionProvider(ObjectMapper json,
            @Value("${knowledge-collector.extraction.playwright.enabled:false}") boolean enabled,
            @Value("${knowledge-collector.extraction.playwright.base-url:http://127.0.0.1:3003}") String endpoint) {
        this.json = json;
        this.configuration = new RuntimeConfiguration(enabled, normalize(endpoint), null, "NONE", null);
    }
    @Override public String id() { return "playwright"; }
    @Override public String serviceType() { return "EXTRACTION"; }
    @Override public String displayName() { return "Playwright 浏览器抓取"; }
    @Override public String implementationName() { return getClass().getName(); }
    @Override public List<String> businessUsages() { return List.of("动态网页正文抓取", "网页截图", "失败采集重试"); }
    @Override public RuntimeConfiguration currentConfiguration() { return configuration; }
    @Override public synchronized void configure(RuntimeConfiguration value) {
        configuration = new RuntimeConfiguration(value.enabled(), normalize(value.endpoint()), value.model(), value.authenticationType(), value.credential());
    }
    @Override public ConnectionResult testConnection() {
        if (!configuration.enabled()) return new ConnectionResult(false, "Playwright 服务已停用", List.of());
        try {
            var response = http.send(HttpRequest.newBuilder(URI.create(configuration.endpoint() + "/health"))
                    .timeout(Duration.ofSeconds(8)).GET().build(), HttpResponse.BodyHandlers.discarding());
            boolean ok = response.statusCode() / 100 == 2;
            return new ConnectionResult(ok, ok ? "Playwright 浏览器服务连接成功" : "Playwright 返回 HTTP " + response.statusCode(), List.of("chromium"));
        } catch (Exception e) { return new ConnectionResult(false, "无法连接 Playwright：" + safe(e), List.of()); }
    }
    @Override public ExtractionResult extract(ExtractionRequest request) {
        if (!configuration.enabled()) throw new IllegalStateException("PLAYWRIGHT-DISABLED: Playwright 服务已停用");
        try {
            String payload = json.writeValueAsString(Map.of("url", request.url(), "timeoutSeconds", request.timeoutSeconds(), "screenshot", true));
            HttpResponse<String> response = http.send(HttpRequest.newBuilder(URI.create(configuration.endpoint() + "/render"))
                    .timeout(Duration.ofSeconds(Math.max(5, request.timeoutSeconds()) + 5)).header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload)).build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() / 100 != 2) throw new IllegalStateException("PLAYWRIGHT-HTTP-" + response.statusCode() + ": " + response.body());
            JsonNode data = json.readTree(response.body());
            String raw = text(data, "html");
            String clean = raw == null ? null : Jsoup.clean(raw, request.url(), Safelist.relaxed());
            String shot = text(data, "screenshotBase64");
            return new ExtractionResult(or(text(data, "finalUrl"), request.url()), text(data, "title"), text(data, "author"),
                    date(text(data, "publishedAt")), clean, clean == null ? null : Jsoup.parse(clean).text(), raw,
                    shot == null ? null : Base64.getDecoder().decode(shot), Map.of("provider", id(), "browser", "chromium"));
        } catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IllegalStateException("PLAYWRIGHT-INTERRUPTED", e); }
        catch (Exception e) { throw new IllegalStateException("PLAYWRIGHT-EXTRACTION-FAILED: " + safe(e), e); }
    }
    private String normalize(String value) { URI uri = URI.create(value.endsWith("/") ? value.substring(0, value.length() - 1) : value); if (!List.of("http","https").contains(uri.getScheme())) throw new IllegalArgumentException("Playwright 地址只允许 HTTP/HTTPS"); return uri.toString(); }
    private String text(JsonNode node,String name){String value=node.path(name).asText(null);return value==null||value.isBlank()?null:value;}
    private String or(String a,String b){return a==null?b:a;}
    private OffsetDateTime date(String value){try{return value==null?null:OffsetDateTime.parse(value);}catch(Exception ignored){return null;}}
    private String safe(Exception e){return e.getMessage()==null?e.getClass().getSimpleName():e.getMessage();}
}
