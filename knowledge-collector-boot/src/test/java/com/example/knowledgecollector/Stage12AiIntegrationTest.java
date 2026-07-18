package com.example.knowledgecollector;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"test", "local"})
class Stage12AiIntegrationTest {
    private static final WireMockServer OLLAMA = new WireMockServer(options().dynamicPort());

    static {
        OLLAMA.start();
        OLLAMA.stubFor(get(urlEqualTo("/api/tags")).willReturn(aResponse().withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"models\":[{\"name\":\"qwen-test\",\"model\":\"qwen-test\"}]}")));
        OLLAMA.stubFor(post(urlEqualTo("/api/generate")).willReturn(aResponse().withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                        {
                          "model":"qwen-test",
                          "response":"{\\"oneSentenceSummary\\":\\"文章解释了睡眠与主动回忆如何帮助长期记忆。\\",\\"coreSummary\\":\\"睡眠巩固与主动提取共同改善长期记忆。\\",\\"outline\\":[\\"主动回忆\\",\\"睡眠巩固\\"],\\"keyPoints\\":[\\"主动回忆增强提取能力\\",\\"睡眠帮助记忆巩固\\"],\\"keyConclusions\\":[\\"两种方法应结合\\"],\\"keywords\\":[\\"记忆\\",\\"睡眠\\"],\\"tags\\":[\\"认知科学\\"],\\"category\\":\\"学习科学\\",\\"articleType\\":\\"科普\\",\\"readingValue\\":91,\\"qualityScore\\":88,\\"sourceCredibility\\":\\"待人工核验\\",\\"readingReason\\":\\"提供可执行方法\\",\\"informationNature\\":[\\"客观事实\\"],\\"recommendedCards\\":[{\\"title\\":\\"主动回忆\\",\\"content\\":\\"主动回忆增强提取能力\\",\\"cardType\\":\\"METHOD\\",\\"sourceQuote\\":\\"主动回忆增强提取能力\\",\\"confidence\\":80}]}",
                          "done":true,
                          "total_duration":250000000,
                          "prompt_eval_count":120,
                          "eval_count":80
                        }
                        """)));
    }

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",
                () -> "jdbc:h2:mem:stage12-ai-test;DB_CLOSE_DELAY=-1");
        registry.add("knowledge-collector.ai.ollama.base-url", OLLAMA::baseUrl);
        registry.add("knowledge-collector.ai.ollama.model", () -> "qwen-test");
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @LocalServerPort
    private int port;

    @AfterAll
    static void stopServer() {
        OLLAMA.stop();
    }

    @Test
    void analyzesPersistsAndRendersArticleAiResult() {
        JsonNode topic = postJson("/api/v1/topics", Map.of(
                "code", "stage12_ai", "name", "Stage 12 AI", "description", "AI test",
                "keywords", "记忆,睡眠", "excludedKeywords", "", "color", "#6D4AFF",
                "icon", "brain", "language", "zh-CN", "enabled", true, "sortOrder", 1));
        long topicId = topic.path("data").path("id").asLong();
        JsonNode source = postJson("/api/v1/sources", Map.ofEntries(
                Map.entry("code", "stage12_fixture"), Map.entry("name", "Stage 12 Fixture"),
                Map.entry("type", "JSON_API"), Map.entry("homeUrl", "https://fixture.local"),
                Map.entry("feedUrl", "https://fixture.local/feed"), Map.entry("language", "zh-CN"),
                Map.entry("charset", "UTF-8"), Map.entry("userAgent", "test"),
                Map.entry("timeoutSeconds", 5), Map.entry("maxRetries", 0),
                Map.entry("requestIntervalMillis", 0), Map.entry("obeyRobots", true),
                Map.entry("fetchFullContent", true), Map.entry("summaryOnly", false),
                Map.entry("saveSnapshot", false), Map.entry("enabled", true),
                Map.entry("notes", "stage12"), Map.entry("topicIds", new long[]{topicId})
        ));
        long sourceId = source.path("data").path("id").asLong();
        postJson("/api/v1/sources/" + sourceId + "/crawl", null);
        long articleId = getJson("/api/v1/articles?sourceId=" + sourceId)
                .path("data").path("content").get(0).path("id").asLong();

        JsonNode providers = getJson("/api/v1/ai/providers");
        assertThat(providers.path("data").get(0).path("available").asBoolean()).isTrue();
        JsonNode analysis = postJson("/api/v1/articles/" + articleId + "/ai/analyze?provider=ollama", null);
        assertThat(analysis.path("data").path("oneSentenceSummary").asText()).contains("长期记忆");
        assertThat(analysis.path("data").path("readingValue").asInt()).isEqualTo(91);
        assertThat(analysis.path("data").path("keyPoints").size()).isEqualTo(2);
        assertThat(analysis.path("data").path("coreSummary").asText()).contains("睡眠巩固");
        JsonNode recommended = getJson("/api/v1/knowledge/cards?articleId=" + articleId + "&confirmed=false");
        assertThat(recommended.path("data").size()).isEqualTo(1);
        assertThat(recommended.path("data").get(0).path("aiSuggested").asBoolean()).isTrue();

        JsonNode saved = getJson("/api/v1/articles/" + articleId + "/ai");
        assertThat(saved.path("data").path("model").asText()).isEqualTo("qwen-test");
        assertThat(saved.path("data").path("promptTokens").asInt()).isEqualTo(120);
        assertThat(restTemplate.getForObject(url("/articles/" + articleId), String.class))
                .contains("AI 阅读助手", "使用默认 AI 分析");
        assertThat(restTemplate.getForObject(url("/operations"), String.class))
                .contains("内容智能服务", "AI PROVIDERS");
    }

    private JsonNode getJson(String path) {
        return restTemplate.getForObject(url(path), JsonNode.class);
    }

    private JsonNode postJson(String path, Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<JsonNode> response = restTemplate.exchange(url(path), HttpMethod.POST,
                new HttpEntity<>(body, headers), JsonNode.class);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        return response.getBody();
    }

    private String url(String path) {
        return "http://127.0.0.1:" + port + path;
    }
}
