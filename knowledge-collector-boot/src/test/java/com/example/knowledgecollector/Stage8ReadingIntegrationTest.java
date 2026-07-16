package com.example.knowledgecollector;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"test", "local"})
class Stage8ReadingIntegrationTest {
    @LocalServerPort
    int port;
    @Autowired
    TestRestTemplate rest;
    @Autowired
    ObjectMapper mapper;

    @Test
    void completesTopicCollectorCrawlSearchAndReadingWorkflow() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        JsonNode topic = request(HttpMethod.POST, "/api/v1/topics", Map.ofEntries(
                Map.entry("code", "STAGE8_" + suffix), Map.entry("name", "Stage 8 认知 " + suffix),
                Map.entry("description", "reading workflow"), Map.entry("keywords", "记忆,睡眠,主动回忆"),
                Map.entry("excludedKeywords", "广告"), Map.entry("color", "#7C3AED"),
                Map.entry("icon", "brain"), Map.entry("language", "zh-CN"),
                Map.entry("enabled", true), Map.entry("sortOrder", 10)
        ));
        long topicId = topic.path("data").path("id").asLong();

        JsonNode source = request(HttpMethod.POST, "/api/v1/sources", Map.ofEntries(
                Map.entry("code", "LOCAL_" + suffix), Map.entry("name", "本地采集员 " + suffix),
                Map.entry("type", "JSON_API"), Map.entry("homeUrl", "https://fixture.local"),
                Map.entry("feedUrl", "https://fixture.local/" + suffix), Map.entry("language", "zh-CN"),
                Map.entry("charset", "UTF-8"), Map.entry("userAgent", "Stage8Test/1.0"),
                Map.entry("timeoutSeconds", 5), Map.entry("maxRetries", 0),
                Map.entry("requestIntervalMillis", 0), Map.entry("obeyRobots", true),
                Map.entry("fetchFullContent", true), Map.entry("summaryOnly", false),
                Map.entry("saveSnapshot", false), Map.entry("enabled", true),
                Map.entry("notes", "fixture"), Map.entry("topicIds", new long[]{topicId})
        ));
        long sourceId = source.path("data").path("id").asLong();

        JsonNode task = request(HttpMethod.POST, "/api/v1/sources/" + sourceId + "/crawl", null);
        assertThat(task.path("data").path("status").asText()).isEqualTo("SUCCESS");
        assertThat(task.path("data").path("createdCount").asInt()).isEqualTo(1);

        JsonNode articles = get("/api/v1/articles?sourceId=" + sourceId + "&topicId=" + topicId
                + "&minQuality=60&archived=false");
        assertThat(articles.path("data").path("totalElements").asInt()).isEqualTo(1);
        long articleId = articles.path("data").path("content").get(0).path("id").asLong();

        request(HttpMethod.PATCH, "/api/v1/articles/" + articleId + "/reading/state",
                Map.of("readingStatus", "READ", "favorite", true, "archived", false));
        request(HttpMethod.PUT, "/api/v1/articles/" + articleId + "/reading/tags",
                Map.of("tagNames", "认知科学,精读"));
        request(HttpMethod.PUT, "/api/v1/articles/" + articleId + "/reading/note",
                Map.of("content", "复核主动回忆与睡眠的关系"));

        JsonNode reading = get("/api/v1/articles/" + articleId + "/reading");
        assertThat(reading.path("data").path("readingStatus").asText()).isEqualTo("READ");
        assertThat(reading.path("data").path("favorite").asBoolean()).isTrue();
        assertThat(reading.path("data").path("tags")).hasSize(2);
        assertThat(reading.path("data").path("note").asText()).contains("主动回忆");

        JsonNode filtered = get("/api/v1/articles?keyword=睡眠&readingStatus=READ&favorite=true"
                + "&archived=false&tagId=" + reading.path("data").path("tags").get(0).path("id").asLong());
        assertThat(filtered.path("data").path("totalElements").asInt()).isEqualTo(1);
        assertThat(rest.getForEntity(url("/articles"), String.class).getBody())
                .contains("你的资料库", "全文搜索");
        assertThat(rest.getForEntity(url("/articles/" + articleId), String.class).getBody())
                .contains("阅读操作", "个人笔记", "主题与质量");
    }

    private JsonNode get(String path) {
        return request(HttpMethod.GET, path, null);
    }

    private JsonNode request(HttpMethod method, String path, Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        var response = rest.exchange(url(path), method, new HttpEntity<>(body, headers), String.class);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        try {
            return mapper.readTree(response.getBody());
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private String url(String path) {
        return "http://127.0.0.1:" + port + path;
    }
}
