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
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class Stage6HtmlCollectionIntegrationTest {
    private static final WireMockServer SITE = new WireMockServer(options().dynamicPort());

    static {
        SITE.start();
    }

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:stage6-html-test;DB_CLOSE_DELAY=-1");
        registry.add("knowledge-collector.network.enabled", () -> true);
        registry.add("knowledge-collector.network.allow-loopback", () -> true);
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @LocalServerPort
    private int port;

    @BeforeAll
    static void stubSite() throws IOException {
        html("/list.html", "fixtures/html/list.html");
        html("/articles/memory.html", "fixtures/html/memory.html");
        html("/articles/sleep.html", "fixtures/html/sleep.html");
    }

    @AfterAll
    static void stopSite() {
        SITE.stop();
    }

    @Test
    void versionsTestsAndRunsHtmlRuleWithSanitizedContent() {
        JsonNode source = post("/api/v1/sources", Map.ofEntries(
                Map.entry("code", "stage6_html"), Map.entry("name", "Stage 6 HTML"),
                Map.entry("type", "HTML_LIST"), Map.entry("homeUrl", SITE.baseUrl()),
                Map.entry("feedUrl", SITE.baseUrl() + "/list.html"), Map.entry("language", "en"),
                Map.entry("charset", "UTF-8"), Map.entry("userAgent", "KnowledgeCollector-Test/1.0"),
                Map.entry("timeoutSeconds", 5), Map.entry("maxRetries", 0),
                Map.entry("requestIntervalMillis", 0), Map.entry("obeyRobots", true),
                Map.entry("fetchFullContent", true), Map.entry("summaryOnly", false),
                Map.entry("saveSnapshot", false), Map.entry("enabled", true),
                Map.entry("notes", "html fixture"), Map.entry("topicIds", new long[]{})
        ), 201);
        long sourceId = source.path("data").path("id").asLong();

        JsonNode rule = post("/api/v1/sources/" + sourceId + "/rules", Map.of(
                "listSelector", ".article-item", "linkSelector", "a.title",
                "titleSelector", "h1", "contentSelector", ".content",
                "authorSelector", ".author", "publishTimeSelector", "time",
                "datePattern", "", "removeSelectors", ".advertisement\nscript", "enabled", true
        ), 201);
        assertThat(rule.path("data").path("version").asInt()).isEqualTo(1);

        JsonNode preview = post("/api/v1/sources/" + sourceId + "/rules/test", null, 200);
        assertThat(preview.path("data").path("entryCount").asInt()).isEqualTo(2);

        JsonNode task = post("/api/v1/sources/" + sourceId + "/crawl", null, 200);
        assertThat(task.path("data").path("status").asText()).isEqualTo("SUCCESS");
        assertThat(task.path("data").path("createdCount").asInt()).isEqualTo(2);

        JsonNode articles = getJson("/api/v1/articles?sourceId=" + sourceId);
        assertThat(articles.path("data").path("totalElements").asInt()).isEqualTo(2);
        JsonNode first = articles.path("data").path("content").get(0);
        JsonNode detail = getJson("/api/v1/articles/" + first.path("id").asLong());
        assertThat(detail.path("data").path("contentText").asText()).isNotBlank();
        assertThat(detail.path("data").path("contentHtml").asText())
                .doesNotContain("<script", "Advertisement");

        String page = restTemplate.getForObject(url("/sources/" + sourceId + "/rules"), String.class);
        assertThat(page).contains("HTML 采集规则", "/js/source-rules.js");
    }

    private static void html(String path, String resource) throws IOException {
        String body = new ClassPathResource(resource).getContentAsString(StandardCharsets.UTF_8);
        SITE.stubFor(get(urlEqualTo(path)).willReturn(aResponse().withStatus(200)
                .withHeader("Content-Type", "text/html; charset=UTF-8").withBody(body)));
    }

    private JsonNode getJson(String path) {
        return restTemplate.getForObject(url(path), JsonNode.class);
    }

    private JsonNode post(String path, Object body, int expectedStatus) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<JsonNode> response = restTemplate.exchange(url(path), HttpMethod.POST,
                new HttpEntity<>(body, headers), JsonNode.class);
        assertThat(response.getStatusCode().value()).isEqualTo(expectedStatus);
        return response.getBody();
    }

    private String url(String path) {
        return "http://127.0.0.1:" + port + path;
    }
}
