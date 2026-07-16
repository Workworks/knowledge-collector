package com.example.knowledgecollector;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class Stage4ManagementIntegrationTest {

    private static final Path TEST_DATA = createTestDataDirectory();

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("knowledge-collector.storage.root", () -> TEST_DATA.toString());
        registry.add("spring.datasource.url",
                () -> "jdbc:h2:mem:stage4-management-test;DB_CLOSE_DELAY=-1");
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @LocalServerPort
    private int port;

    @Test
    void completesTopicAndSourceCrudWithRelationship() {
        JsonNode topic = post("/api/v1/topics", Map.of(
                "code", "stage4_ai", "name", "Stage 4 人工智能", "description", "集成测试主题",
                "keywords", "AI，Java\nSpring", "excludedKeywords", "广告",
                "color", "#2563EB", "icon", "cpu", "language", "zh-CN",
                "enabled", true, "sortOrder", 10
        ), HttpStatus.CREATED);
        long topicId = topic.path("data").path("id").asLong();
        assertThat(topic.path("data").path("code").asText()).isEqualTo("STAGE4_AI");
        assertThat(topic.path("data").path("keywords").size()).isEqualTo(3);

        JsonNode source = post("/api/v1/sources", Map.ofEntries(
                Map.entry("code", "stage4_feed"), Map.entry("name", "Stage 4 示例订阅"),
                Map.entry("type", "RSS"), Map.entry("homeUrl", "https://example.com"),
                Map.entry("feedUrl", "https://example.com/feed.xml"), Map.entry("language", "zh-CN"),
                Map.entry("charset", "UTF-8"), Map.entry("userAgent", "KnowledgeCollector-Test/1.0"),
                Map.entry("timeoutSeconds", 15), Map.entry("maxRetries", 2),
                Map.entry("requestIntervalMillis", 2000), Map.entry("obeyRobots", true),
                Map.entry("fetchFullContent", true), Map.entry("summaryOnly", false),
                Map.entry("saveSnapshot", false), Map.entry("enabled", true),
                Map.entry("notes", "integration"), Map.entry("topicIds", new long[]{topicId})
        ), HttpStatus.CREATED);
        long sourceId = source.path("data").path("id").asLong();
        assertThat(source.path("data").path("topicIds").get(0).asLong()).isEqualTo(topicId);

        JsonNode sourcePage = get("/api/v1/sources?topicId=" + topicId);
        assertThat(sourcePage.path("data").path("totalElements").asLong()).isEqualTo(1);

        ResponseEntity<JsonNode> conflict = exchange("/api/v1/topics/" + topicId,
                HttpMethod.DELETE, null);
        assertThat(conflict.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(conflict.getBody().path("error").path("code").asText()).isEqualTo("TOPIC-IN-USE");

        JsonNode disabled = exchange("/api/v1/sources/" + sourceId + "/enabled",
                HttpMethod.PATCH, Map.of("enabled", false)).getBody();
        assertThat(disabled.path("data").path("enabled").asBoolean()).isFalse();

        ResponseEntity<JsonNode> sourceTest = exchange("/api/v1/sources/" + sourceId + "/test",
                HttpMethod.POST, null);
        assertThat(sourceTest.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(sourceTest.getBody().path("error").path("code").asText())
                .isEqualTo("SOURCE-TEST-NOT-AVAILABLE");

        assertThat(exchange("/api/v1/sources/" + sourceId, HttpMethod.DELETE, null).getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(exchange("/api/v1/topics/" + topicId, HttpMethod.DELETE, null).getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void rendersTopicAndSourceManagementPages() {
        assertThat(restTemplate.getForEntity(url("/topics"), String.class).getBody())
                .contains("主题管理", "/js/topics.js");
        assertThat(restTemplate.getForEntity(url("/sources"), String.class).getBody())
                .contains("采集源管理", "/js/sources.js");
    }

    private JsonNode get(String path) {
        return restTemplate.getForObject(url(path), JsonNode.class);
    }

    private JsonNode post(String path, Object body, HttpStatus expected) {
        ResponseEntity<JsonNode> response = exchange(path, HttpMethod.POST, body);
        assertThat(response.getStatusCode()).isEqualTo(expected);
        return response.getBody();
    }

    private ResponseEntity<JsonNode> exchange(String path, HttpMethod method, Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Correlation-Id", "stage4-integration-test");
        return restTemplate.exchange(url(path), method, new HttpEntity<>(body, headers), JsonNode.class);
    }

    private String url(String path) {
        return "http://127.0.0.1:" + port + path;
    }

    private static Path createTestDataDirectory() {
        try {
            return Files.createTempDirectory("knowledge-collector-stage4-");
        } catch (IOException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }
}
