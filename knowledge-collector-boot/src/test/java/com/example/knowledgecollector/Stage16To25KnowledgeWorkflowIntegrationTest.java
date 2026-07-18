package com.example.knowledgecollector;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import java.time.OffsetDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.datasource.url=jdbc:h2:mem:stage16to25;DB_CLOSE_DELAY=-1")
@ActiveProfiles({"test", "local"})
class Stage16To25KnowledgeWorkflowIntegrationTest {
    @Autowired TestRestTemplate rest;
    @LocalServerPort int port;

    @Test
    void buildsTraceableKnowledgeResearchWritingAndReviewChain() {
        JsonNode first = post("/api/v1/knowledge/cards", Map.of(
                "title", "RAG 的核心作用", "content", "检索外部证据后再生成答案。", "cardType", "FACT",
                "tags", "RAG,事实核验", "aiSuggested", true));
        long firstId = first.path("data").path("id").asLong();
        assertThat(first.path("data").path("confirmed").asBoolean()).isFalse();
        assertThat(post("/api/v1/knowledge/cards/" + firstId + "/confirm", null)
                .path("data").path("confirmed").asBoolean()).isTrue();
        long secondId = post("/api/v1/knowledge/cards", Map.of(
                "title", "工具调用", "content", "模型通过工具获取实时信息。", "cardType", "METHOD"))
                .path("data").path("id").asLong();
        assertThat(post("/api/v1/knowledge/cards/" + firstId + "/relations/" + secondId,
                Map.of("relationType", "SUPPLEMENTS", "note", "两种事实增强方式"))
                .path("data").path("relationType").asText()).isEqualTo("SUPPLEMENTS");

        long claimId = post("/api/v1/knowledge/claims", Map.of(
                "statement", "RAG 能降低无来源回答的风险", "scopeNote", "拥有可靠知识库时"))
                .path("data").path("id").asLong();
        assertThat(post("/api/v1/knowledge/claims/" + claimId + "/evidence", Map.of(
                "cardId", firstId, "evidenceType", "SUPPORT", "excerpt", "检索后生成", "strength", 80))
                .path("data").path("claimId").asLong()).isEqualTo(claimId);

        long canonical = post("/api/v1/knowledge/entities", Map.of(
                "canonicalName", "OpenAI", "entityType", "COMPANY", "aliases", "Open AI,开放人工智能"))
                .path("data").path("id").asLong();
        long alias = post("/api/v1/knowledge/entities", Map.of(
                "canonicalName", "Open AI", "entityType", "COMPANY"))
                .path("data").path("id").asLong();
        assertThat(post("/api/v1/knowledge/entities/" + alias + "/merge/" + canonical, null)
                .path("data").path("mergedIntoId").asLong()).isEqualTo(canonical);

        assertThat(post("/api/v1/knowledge/events", Map.of(
                "title", "AI Agent 产品发布", "summary", "多来源事件聚合", "eventDate", "2026-07-18",
                "clusterKey", "ai-agent-release")).path("data").path("status").asText()).isEqualTo("TRACKING");
        long topicId = post("/api/v1/knowledge/topics", Map.of(
                "name", "AI Agent", "introduction", "持续更新的专题", "unresolvedQuestions", "可靠性如何评估？"))
                .path("data").path("id").asLong();
        long projectId = post("/api/v1/knowledge/projects", Map.of(
                "title", "未来五年 AI Agent", "objective", "研究发展方向", "coreQuestions", "关键能力是什么？"))
                .path("data").path("id").asLong();
        assertThat(post("/api/v1/knowledge/projects/" + projectId + "/items", Map.of(
                "cardId", firstId, "usageType", "CORE_EVIDENCE", "note", "核心材料"))
                .path("data").path("projectId").asLong()).isEqualTo(projectId);
        assertThat(post("/api/v1/knowledge/syntheses", Map.of(
                "projectId", projectId, "topicPageId", topicId, "title", "Agent 研究简报",
                "synthesisType", "RESEARCH_BRIEF", "content", "共同结论与主要分歧",
                "sourceReferences", "card:" + firstId + ",claim:" + claimId))
                .path("data").path("sourceReferences").asText()).contains("card:");
        assertThat(post("/api/v1/knowledge/drafts", Map.of(
                "projectId", projectId, "title", "AI Agent 的下一阶段", "outline", "一、背景\n二、证据",
                "content", "带来源的草稿", "sourceReferences", "card:" + firstId))
                .path("data").path("status").asText()).isEqualTo("DRAFT");
        assertThat(post("/api/v1/knowledge/gaps", Map.of(
                "projectId", projectId, "gapType", "MISSING_COUNTERPOINT", "description", "需要补充反方资料"))
                .path("data").path("status").asText()).isEqualTo("OPEN");

        long reviewId = post("/api/v1/knowledge/cards/" + firstId + "/reviews", Map.of(
                "reviewType", "SPACED", "dueAt", OffsetDateTime.now().minusMinutes(1).toString()))
                .path("data").path("id").asLong();
        assertThat(get("/api/v1/knowledge/reviews/due").path("data").size()).isEqualTo(1);
        assertThat(post("/api/v1/knowledge/reviews/" + reviewId + "/complete", Map.of("nextReviewDays", 7))
                .path("data").path("reviewCount").asInt()).isEqualTo(1);

        JsonNode stats = get("/api/v1/knowledge/statistics").path("data");
        assertThat(stats.path("cards").asLong()).isEqualTo(2);
        assertThat(stats.path("claims").asLong()).isEqualTo(1);
        assertThat(stats.path("projects").asLong()).isEqualTo(1);
        assertThat(get("/api/v1/knowledge/entities").path("data").size()).isEqualTo(1);
        assertThat(get("/api/v1/knowledge/events").path("data").get(0).path("articleCount").asLong()).isZero();
        assertThat(get("/api/v1/knowledge/topics").path("data").size()).isEqualTo(1);
        assertThat(get("/api/v1/knowledge/syntheses").path("data").size()).isEqualTo(1);
        assertThat(get("/api/v1/knowledge/drafts").path("data").size()).isEqualTo(1);
        assertThat(rest.getForObject(url("/knowledge"), String.class))
                .contains("知识研究工作台", "STAGE 16–25", "多资料综合归纳", "知识复习与持续更新");
    }

    private JsonNode get(String path) { return rest.getForObject(url(path), JsonNode.class); }
    private JsonNode post(String path, Object body) {
        HttpHeaders headers = new HttpHeaders(); headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<JsonNode> response = rest.exchange(url(path), HttpMethod.POST,
                new HttpEntity<>(body, headers), JsonNode.class);
        assertThat(response.getStatusCode().is2xxSuccessful()).as(path).isTrue();
        return response.getBody();
    }
    private String url(String path) { return "http://127.0.0.1:" + port + path; }
}
