package com.example.knowledgecollector;

import com.example.knowledgecollector.capability.source.ContentSourceProvider;
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

import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class Stage13AiChatWorkflowTest {
    private static final WireMockServer OLLAMA = new WireMockServer(options().dynamicPort());

    static {
        OLLAMA.start();
        OLLAMA.stubFor(post(urlEqualTo("/api/chat")).willReturn(aResponse().withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                        {
                          "model":"deepseek-test",
                          "message":{"role":"assistant","content":"主动回忆通过反复提取信息强化记忆痕迹，适合整理为学习方法资料。"},
                          "total_duration":320000000,
                          "prompt_eval_count":42,
                          "eval_count":28
                        }
                        """)));
    }

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:stage13-chat;DB_CLOSE_DELAY=-1");
        registry.add("knowledge-collector.ai.ollama.base-url", OLLAMA::baseUrl);
        registry.add("knowledge-collector.ai.ollama.model", () -> "deepseek-test");
    }

    @Autowired TestRestTemplate restTemplate;
    @Autowired List<ContentSourceProvider> sourceProviders;
    @LocalServerPort int port;

    @AfterAll
    static void stop() {
        OLLAMA.stop();
    }

    @Test
    void chatsAndSavesAssistantMaterialForReview() {
        assertThat(sourceProviders).anyMatch(provider -> provider.supports("MANUAL_URL"));
        JsonNode conversation = postJson("/api/v1/ai/chat/conversations", Map.of());
        long conversationId = conversation.path("data").path("id").asLong();
        JsonNode reply = postJson("/api/v1/ai/chat/conversations/" + conversationId + "/messages",
                Map.of("content", "请解释主动回忆"));
        long messageId = reply.path("data").path("id").asLong();
        assertThat(reply.path("data").path("content").asText()).contains("强化记忆");

        JsonNode material = postJson("/api/v1/ai/chat/messages/" + messageId + "/save",
                Map.of("title", "主动回忆学习方法"));
        long articleId = material.path("data").path("articleId").asLong();
        JsonNode article = getJson("/api/v1/articles/" + articleId);
        assertThat(article.path("data").path("contentOrigin").asText()).isEqualTo("AI_GENERATED");
        assertThat(article.path("data").path("reviewStatus").asText()).isEqualTo("PENDING_REVIEW");
        assertThat(restTemplate.getForObject(url("/ai-chat"), String.class))
                .contains("AI 研究助手", "AI 生成内容应在入库后人工审核");
        assertThat(restTemplate.getForObject(url("/articles/" + articleId), String.class))
                .contains("AI 生成内容", "该资料由 AI 生成");
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
