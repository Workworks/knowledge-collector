package com.example.knowledgecollector;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
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
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class Stage5FeedCollectionIntegrationTest {
    private static final WireMockServer FEED_SERVER = new WireMockServer(options().dynamicPort());

    static {
        FEED_SERVER.start();
    }

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",
                () -> "jdbc:h2:mem:stage5-feed-test;DB_CLOSE_DELAY=-1");
        registry.add("knowledge-collector.network.enabled", () -> true);
        registry.add("knowledge-collector.network.allow-loopback", () -> true);
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @LocalServerPort
    private int port;

    @BeforeAll
    static void stubFeed() {
        FEED_SERVER.stubFor(get(urlEqualTo("/feed.xml")).willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/rss+xml; charset=UTF-8")
                .withBody("""
                        <?xml version="1.0" encoding="UTF-8"?>
                        <rss version="2.0"><channel><title>Stage 5 Feed</title>
                        <link>https://example.test</link><description>fixture</description>
                        <item><title>First article</title><link>https://example.test/posts/1?utm_source=test</link>
                        <description>First summary</description><pubDate>Wed, 16 Jul 2025 08:00:00 GMT</pubDate></item>
                        <item><title>Second article</title><link>https://example.test/posts/2</link>
                        <description>Second summary</description></item>
                        </channel></rss>
                        """)));
    }

    @AfterAll
    static void stopServer() {
        FEED_SERVER.stop();
    }

    @Test
    void testsCollectsDeduplicatesAndQueriesArticles() {
        JsonNode source = post("/api/v1/sources", Map.ofEntries(
                Map.entry("code", "stage5_rss"), Map.entry("name", "Stage 5 RSS"),
                Map.entry("type", "RSS"), Map.entry("homeUrl", FEED_SERVER.baseUrl()),
                Map.entry("feedUrl", FEED_SERVER.baseUrl() + "/feed.xml"),
                Map.entry("language", "zh-CN"), Map.entry("charset", "UTF-8"),
                Map.entry("userAgent", "KnowledgeCollector-Test/1.0"),
                Map.entry("timeoutSeconds", 5), Map.entry("maxRetries", 0),
                Map.entry("requestIntervalMillis", 0), Map.entry("obeyRobots", true),
                Map.entry("fetchFullContent", false), Map.entry("summaryOnly", true),
                Map.entry("saveSnapshot", false), Map.entry("enabled", true),
                Map.entry("notes", "wiremock"), Map.entry("topicIds", new long[]{})
        ));
        long sourceId = source.path("data").path("id").asLong();

        JsonNode tested = post("/api/v1/sources/" + sourceId + "/test", null);
        assertThat(tested.path("data").path("entryCount").asInt()).isEqualTo(2);

        JsonNode first = post("/api/v1/sources/" + sourceId + "/crawl", null);
        assertThat(first.path("data").path("status").asText()).isEqualTo("SUCCESS");
        assertThat(first.path("data").path("createdCount").asInt()).isEqualTo(2);

        JsonNode second = post("/api/v1/sources/" + sourceId + "/crawl", null);
        assertThat(second.path("data").path("duplicateCount").asInt()).isEqualTo(2);

        JsonNode articles = fetch("/api/v1/articles?sourceId=" + sourceId);
        assertThat(articles.path("data").path("totalElements").asInt()).isEqualTo(2);
        long articleId = articles.path("data").path("content").get(0).path("id").asLong();
        assertThat(fetch("/api/v1/articles/" + articleId).path("data").path("title").asText()).isNotBlank();
        assertThat(fetch("/api/v1/tasks").path("data").path("totalElements").asInt()).isEqualTo(2);

        assertThat(restTemplate.getForEntity(url("/articles"), String.class).getBody()).contains("First article");
        assertThat(restTemplate.getForEntity(url("/articles/" + articleId), String.class).getStatusCode().is2xxSuccessful())
                .isTrue();
    }

    private JsonNode fetch(String path) {
        return restTemplate.getForObject(url(path), JsonNode.class);
    }

    private JsonNode post(String path, Object body) {
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
