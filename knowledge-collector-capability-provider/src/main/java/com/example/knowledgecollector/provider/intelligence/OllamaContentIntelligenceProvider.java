package com.example.knowledgecollector.provider.intelligence;

import com.example.knowledgecollector.capability.intelligence.ContentIntelligenceProvider;
import com.example.knowledgecollector.capability.intelligence.ConversationalIntelligenceProvider;
import com.example.knowledgecollector.capability.management.ManagedCapabilityProvider;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class OllamaContentIntelligenceProvider implements ContentIntelligenceProvider,
        ConversationalIntelligenceProvider, ManagedCapabilityProvider {
    private static final Map<String, Object> FORMAT = Map.of(
            "type", "object",
            "properties", analysisProperties(),
            "required", new String[]{
                    "oneSentenceSummary", "keyPoints", "keywords", "tags", "category", "readingValue"
            }
    );

    private static Map<String, Object> analysisProperties() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("oneSentenceSummary", Map.of("type", "string"));
        properties.put("coreSummary", Map.of("type", "string"));
        for (String name : List.of("outline", "keyPoints", "keyConclusions", "keyData", "importantCases",
                "people", "organizations", "products", "technologies", "locations", "timeInformation",
                "keywords", "tags", "informationNature")) {
            properties.put(name, Map.of("type", "array", "items", Map.of("type", "string")));
        }
        properties.put("category", Map.of("type", "string"));
        properties.put("articleType", Map.of("type", "string"));
        properties.put("readingReason", Map.of("type", "string"));
        properties.put("sourceCredibility", Map.of("type", "string"));
        properties.put("readingValue", Map.of("type", "integer", "minimum", 0, "maximum", 100));
        properties.put("qualityScore", Map.of("type", "integer", "minimum", 0, "maximum", 100));
        properties.put("recommendedCards", Map.of("type", "array", "items", Map.of(
                "type", "object", "properties", Map.of(
                        "title", Map.of("type", "string"),
                        "content", Map.of("type", "string"),
                        "cardType", Map.of("type", "string"),
                        "sourceQuote", Map.of("type", "string"),
                        "confidence", Map.of("type", "integer", "minimum", 0, "maximum", 100)),
                "required", new String[]{"title", "content", "cardType"})));
        return properties;
    }

    private final ObjectMapper json;
    private final HttpClient http;
    private volatile boolean enabled;
    private volatile URI endpoint;
    private volatile String model;
    private final Duration timeout;
    private final int maxContentChars;

    public OllamaContentIntelligenceProvider(
            ObjectMapper json,
            @Value("${knowledge-collector.ai.ollama.enabled:true}") boolean enabled,
            @Value("${knowledge-collector.ai.ollama.base-url:http://127.0.0.1:11434}") String baseUrl,
            @Value("${knowledge-collector.ai.ollama.model:deepseek-r1:14b}") String model,
            @Value("${knowledge-collector.ai.ollama.timeout:PT2M}") Duration timeout,
            @Value("${knowledge-collector.ai.max-content-chars:24000}") int maxContentChars) {
        this.json = json;
        this.enabled = enabled;
        this.endpoint = validateEndpoint(baseUrl);
        this.model = model;
        this.timeout = timeout;
        this.maxContentChars = Math.max(1000, maxContentChars);
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    }

    @Override
    public String id() {
        return "ollama";
    }

    @Override public String serviceType() { return "AI"; }
    @Override public String displayName() { return "Ollama 本地大模型"; }
    @Override public String implementationName() { return getClass().getName(); }
    @Override public List<String> businessUsages() {
        return List.of("文章结构化分析", "AI 研究助手", "知识卡片推荐", "AI 采集源建议");
    }
    @Override public RuntimeConfiguration currentConfiguration() {
        return new RuntimeConfiguration(enabled, endpoint.toString(), model, "NONE", null);
    }

    @Override
    public synchronized void configure(RuntimeConfiguration configuration) {
        this.enabled = configuration.enabled();
        this.endpoint = validateEndpoint(configuration.endpoint());
        if (configuration.model() != null && !configuration.model().isBlank()) {
            this.model = configuration.model().trim();
        }
    }

    @Override
    public ConnectionResult testConnection() {
        ProviderStatus status = status();
        return new ConnectionResult(status.available(), status.message(), availableModels());
    }

    @Override
    public ProviderStatus status() {
        if (!enabled) {
            return new ProviderStatus(id(), false, false, model, endpoint.toString(), "Ollama 已禁用");
        }
        try {
            HttpRequest request = HttpRequest.newBuilder(endpoint.resolve("/api/tags"))
                    .timeout(Duration.ofSeconds(5)).GET().build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            boolean available = response.statusCode() == 200;
            String message = available ? modelMessage(response.body()) : "Ollama HTTP " + response.statusCode();
            return new ProviderStatus(id(), true, available, model, endpoint.toString(), message);
        } catch (Exception exception) {
            return new ProviderStatus(id(), true, false, model, endpoint.toString(),
                    "无法连接 Ollama：" + safeMessage(exception));
        }
    }

    @Override
    public AnalysisResult analyze(AnalysisRequest request) {
        if (!enabled) {
            throw new IllegalStateException("AI-PROVIDER-DISABLED: Ollama 已禁用");
        }
        String content = request.content() == null ? "" : request.content();
        if (content.length() > maxContentChars) {
            content = content.substring(0, maxContentChars);
        }
        String prompt = """
                请分析下面的文章。只根据提供的内容作答，不要补充无法验证的事实。
                输出语言与文章主要语言一致。请生成一句话摘要、核心摘要、大纲、核心观点、关键结论、
                关键数据、重要案例，以及人物、机构、产品、技术、地点、时间信息、关键词和标签。
                同时判断文章类型、内容质量、来源可信度、推荐阅读理由，并逐条标记信息性质：
                客观事实、作者观点、引用观点、推断、预测、广告性表达或尚未证实信息。
                另外推荐 0-6 张可独立复用的知识卡片，类型只能使用 FACT、DATA、DEFINITION、CONCEPT、
                METHOD、CASE、OPINION、CONCLUSION、PREDICTION、QUOTE 或 TO_VERIFY，并保留对应原文摘录。
                不存在的项目返回空数组，不要凭空补充。

                标题：%s

                正文：
                %s
                """.formatted(request.title(), content);
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", model);
            body.put("prompt", prompt);
            body.put("stream", false);
            body.put("format", FORMAT);
            body.put("options", Map.of("temperature", 0));
            HttpRequest httpRequest = HttpRequest.newBuilder(endpoint.resolve("/api/generate"))
                    .timeout(timeout)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body), StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = http.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IllegalStateException("OLLAMA-HTTP-" + response.statusCode() + ": "
                        + errorMessage(response.body()));
            }
            JsonNode envelope = json.readTree(response.body());
            String generated = envelope.path("response").asText();
            if (generated.isBlank()) {
                throw new IllegalStateException("OLLAMA-EMPTY-RESPONSE: 模型未返回分析内容");
            }
            Map<String, Object> values = json.readValue(generated, new TypeReference<>() {
            });
            values.put("model", envelope.path("model").asText(model));
            values.put("promptTokens", envelope.path("prompt_eval_count").asInt(0));
            values.put("responseTokens", envelope.path("eval_count").asInt(0));
            values.put("durationMillis", envelope.path("total_duration").asLong(0) / 1_000_000);
            return new AnalysisResult(Map.copyOf(values), id());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("OLLAMA-INTERRUPTED: AI 分析被中断", exception);
        } catch (IllegalStateException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("OLLAMA-REQUEST-FAILED: " + safeMessage(exception), exception);
        }
    }

    @Override
    public ChatResult chat(ChatRequest request) {
        if (!enabled) {
            throw new IllegalStateException("AI-PROVIDER-DISABLED: Ollama 已禁用");
        }
        try {
            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", """
                    你是 Knowledge Collector 的资料研究助手。请使用清晰、准确的中文回答。
                    不要虚构来源或事实；无法确认时明确说明。输出应适合用户进一步审核并保存到资料库。
                    """));
            for (var message : request.messages()) {
                messages.add(Map.of("role", message.role(), "content", message.content()));
            }
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", model);
            body.put("messages", messages);
            body.put("stream", false);
            body.put("options", Map.of("temperature", 0.2));
            HttpRequest httpRequest = HttpRequest.newBuilder(endpoint.resolve("/api/chat"))
                    .timeout(timeout)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body), StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = http.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IllegalStateException("OLLAMA-CHAT-HTTP-" + response.statusCode() + ": "
                        + errorMessage(response.body()));
            }
            JsonNode envelope = json.readTree(response.body());
            String content = cleanThinking(envelope.path("message").path("content").asText());
            if (content.isBlank()) {
                throw new IllegalStateException("OLLAMA-EMPTY-RESPONSE: 模型未返回聊天内容");
            }
            return new ChatResult(content, id(), envelope.path("model").asText(model),
                    envelope.path("prompt_eval_count").asInt(0),
                    envelope.path("eval_count").asInt(0),
                    envelope.path("total_duration").asLong(0) / 1_000_000);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("OLLAMA-INTERRUPTED: AI 对话被中断", exception);
        } catch (IllegalStateException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("OLLAMA-CHAT-FAILED: " + safeMessage(exception), exception);
        }
    }

    private String cleanThinking(String value) {
        return value == null ? "" : value.replaceAll("(?s)<think>.*?</think>", "").trim();
    }

    private URI validateEndpoint(String baseUrl) {
        URI uri = URI.create(baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl);
        if (!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme())) {
            throw new IllegalArgumentException("Ollama 地址只允许 HTTP/HTTPS");
        }
        return uri;
    }

    private String modelMessage(String responseBody) {
        try {
            for (JsonNode item : json.readTree(responseBody).path("models")) {
                if (model.equals(item.path("name").asText()) || model.equals(item.path("model").asText())) {
                    return "Ollama 可用，模型已安装";
                }
            }
            return "Ollama 可用，但模型尚未安装：" + model;
        } catch (Exception ignored) {
            return "Ollama 可用";
        }
    }

    private List<String> availableModels() {
        if (!enabled) return List.of();
        try {
            HttpRequest request = HttpRequest.newBuilder(endpoint.resolve("/api/tags"))
                    .timeout(Duration.ofSeconds(5)).GET().build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) return List.of();
            List<String> result = new ArrayList<>();
            for (JsonNode item : json.readTree(response.body()).path("models")) {
                String name = item.path("name").asText(item.path("model").asText());
                if (!name.isBlank()) result.add(name);
            }
            return List.copyOf(result);
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private String errorMessage(String body) {
        try {
            return json.readTree(body).path("error").asText(body);
        } catch (Exception ignored) {
            return body;
        }
    }

    private String safeMessage(Exception exception) {
        return exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
    }
}
