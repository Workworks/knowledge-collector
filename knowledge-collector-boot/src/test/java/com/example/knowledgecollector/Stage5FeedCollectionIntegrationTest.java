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
import java.util.LinkedHashMap;

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
        FEED_SERVER.stubFor(get(urlEqualTo("/slow.xml")).willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/rss+xml")
                .withFixedDelay(2500)
                .withBody("<rss version=\"2.0\"><channel><title>slow</title></channel></rss>")));
        FEED_SERVER.stubFor(get(urlEqualTo("/full-feed.xml")).willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/rss+xml; charset=UTF-8")
                .withBody("""
                        <?xml version="1.0" encoding="UTF-8"?>
                        <rss version="2.0"><channel><title>Full content feed</title>
                        <link>%s</link><description>fixture</description>
                        <item><title>记忆研究全文</title><link>%s/posts/full.html</link>
                        <description><![CDATA[这是一段订阅摘要。]]></description></item>
                        </channel></rss>
                        """.formatted(FEED_SERVER.baseUrl(), FEED_SERVER.baseUrl()))));
        FEED_SERVER.stubFor(get(urlEqualTo("/posts/full.html")).willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "text/html; charset=UTF-8")
                .withBody("""
                        <!doctype html><html><body><nav>菜单</nav><main><article>
                        <h1>记忆研究全文</h1><div class="article-content">
                        <p>这是真正从文章详情页提取的第一段正文，介绍主动回忆如何加强长期记忆。</p>
                        <p>第二段正文讨论睡眠、间隔重复和知识巩固之间的关系，并提供足够长度用于正文识别。</p>
                        <script>alert('unsafe')</script><div class="advertisement">广告内容</div>
                        </div></article></main></body></html>
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

        assertThat(restTemplate.getForEntity(url("/articles"), String.class).getBody())
                .contains("你的资料库", "全文搜索");
        assertThat(restTemplate.getForEntity(url("/articles/" + articleId), String.class).getStatusCode().is2xxSuccessful())
                .isTrue();
    }

    @Test
    void timesOutWholeResponseAndReleasesSourceForNextTask() {
        JsonNode source = post("/api/v1/sources", Map.ofEntries(
                Map.entry("code", "stage5_slow"), Map.entry("name", "Stage 5 Slow RSS"),
                Map.entry("type", "RSS"), Map.entry("homeUrl", FEED_SERVER.baseUrl()),
                Map.entry("feedUrl", FEED_SERVER.baseUrl() + "/slow.xml"),
                Map.entry("language", "zh-CN"), Map.entry("charset", "UTF-8"),
                Map.entry("userAgent", "KnowledgeCollector-Test/1.0"),
                Map.entry("timeoutSeconds", 1), Map.entry("maxRetries", 0),
                Map.entry("requestIntervalMillis", 0), Map.entry("obeyRobots", true),
                Map.entry("fetchFullContent", false), Map.entry("summaryOnly", true),
                Map.entry("saveSnapshot", false), Map.entry("enabled", true),
                Map.entry("notes", "timeout fixture"), Map.entry("topicIds", new long[]{})
        ));
        long sourceId = source.path("data").path("id").asLong();

        JsonNode first = post("/api/v1/sources/" + sourceId + "/crawl", null);
        assertThat(first.path("data").path("status").asText()).isEqualTo("FAILED");
        assertThat(first.path("data").path("errorMessage").asText())
                .contains("HTTP-REQUEST-TIMEOUT: 请求在 1 秒内未完成");

        JsonNode second = post("/api/v1/sources/" + sourceId + "/crawl", null);
        assertThat(second.path("data").path("id").asLong()).isNotEqualTo(first.path("data").path("id").asLong());
        assertThat(second.path("data").path("status").asText()).isEqualTo("FAILED");
    }

    @Test
    void fetchesSanitizesAndBackfillsRssArticleBody() {
        Map<String, Object> sourceBody = new LinkedHashMap<>();
        sourceBody.put("code", "stage11_rss_body");
        sourceBody.put("name", "Stage 11 RSS Body");
        sourceBody.put("type", "RSS");
        sourceBody.put("homeUrl", FEED_SERVER.baseUrl());
        sourceBody.put("feedUrl", FEED_SERVER.baseUrl() + "/full-feed.xml");
        sourceBody.put("language", "zh-CN");
        sourceBody.put("charset", "UTF-8");
        sourceBody.put("userAgent", "KnowledgeCollector-Test/1.0");
        sourceBody.put("timeoutSeconds", 5);
        sourceBody.put("maxRetries", 0);
        sourceBody.put("requestIntervalMillis", 0);
        sourceBody.put("obeyRobots", true);
        sourceBody.put("fetchFullContent", false);
        sourceBody.put("summaryOnly", true);
        sourceBody.put("saveSnapshot", false);
        sourceBody.put("enabled", true);
        sourceBody.put("notes", "rss body fixture");
        sourceBody.put("topicIds", new long[]{});

        JsonNode source = post("/api/v1/sources", sourceBody);
        long sourceId = source.path("data").path("id").asLong();
        post("/api/v1/sources/" + sourceId + "/crawl", null);
        long articleId = fetch("/api/v1/articles?sourceId=" + sourceId)
                .path("data").path("content").get(0).path("id").asLong();
        assertThat(fetch("/api/v1/articles/" + articleId).path("data").path("contentText").isNull())
                .isTrue();

        sourceBody.put("fetchFullContent", true);
        sourceBody.put("summaryOnly", false);
        put("/api/v1/sources/" + sourceId, sourceBody);
        JsonNode second = post("/api/v1/sources/" + sourceId + "/crawl", null);
        assertThat(second.path("data").path("duplicateCount").asInt()).isEqualTo(1);

        JsonNode detail = fetch("/api/v1/articles/" + articleId).path("data");
        assertThat(detail.path("contentText").asText())
                .contains("真正从文章详情页提取", "睡眠、间隔重复")
                .doesNotContain("广告内容", "alert");
        assertThat(detail.path("contentHtml").asText())
                .doesNotContain("<script", "advertisement");
        assertThat(restTemplate.getForObject(url("/articles/" + articleId), String.class))
                .contains("真正从文章详情页提取")
                .doesNotContain("暂未采集到正文");
    }

    private JsonNode fetch(String path) {
        return restTemplate.getForObject(url(path), JsonNode.class);
    }

    private JsonNode post(String path, Object body) {
        return exchange(path, HttpMethod.POST, body);
    }

    private JsonNode put(String path, Object body) {
        return exchange(path, HttpMethod.PUT, body);
    }

    private JsonNode exchange(String path, HttpMethod method, Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<JsonNode> response = restTemplate.exchange(url(path), method,
                new HttpEntity<>(body, headers), JsonNode.class);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        return response.getBody();
    }

    private String url(String path) {
        return "http://127.0.0.1:" + port + path;
    }
}
