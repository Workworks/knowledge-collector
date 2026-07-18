package com.example.knowledgecollector.infrastructure.intelligence;

import com.example.knowledgecollector.application.intelligence.ArticleIntelligenceGateway;
import com.example.knowledgecollector.application.intelligence.ArticleIntelligenceView;
import com.example.knowledgecollector.capability.intelligence.ContentIntelligenceProvider;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class JdbcArticleIntelligenceGateway implements ArticleIntelligenceGateway {
    private final JdbcClient jdbc;
    private final ObjectMapper json;

    public JdbcArticleIntelligenceGateway(JdbcClient jdbc, ObjectMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    @Override
    public Optional<ArticleIntelligenceView> find(long articleId) {
        return jdbc.sql("select * from article_ai_analysis where article_id=:articleId")
                .param("articleId", articleId).query((rs, row) -> map(
                        rs.getLong("article_id"), rs.getString("provider"), rs.getString("model"),
                        rs.getString("status"), rs.getString("result_json"),
                        rs.getString("error_message"), rs.getObject("analyzed_at", OffsetDateTime.class)))
                .optional();
    }

    @Override
    @Transactional
    public ArticleIntelligenceView saveSuccess(long articleId,
                                                ContentIntelligenceProvider.AnalysisResult result) {
        Map<String, Object> values = result.values();
        return save(articleId, result.provider(), text(values, "model"), "SUCCESS", values, null);
    }

    @Override
    @Transactional
    public ArticleIntelligenceView saveFailure(long articleId, String provider, String message) {
        return save(articleId, provider, null, "FAILED", Map.of(), message);
    }

    private ArticleIntelligenceView save(long articleId, String provider, String model, String status,
                                         Map<String, Object> values, String errorMessage) {
        OffsetDateTime now = OffsetDateTime.now();
        String resultJson;
        try {
            resultJson = json.writeValueAsString(values);
        } catch (Exception exception) {
            throw new IllegalStateException("AI 结果序列化失败", exception);
        }
        jdbc.sql("delete from article_ai_analysis where article_id=:articleId")
                .param("articleId", articleId).update();
        jdbc.sql("""
                insert into article_ai_analysis(article_id,provider,model,status,result_json,
                error_message,analyzed_at,updated_at)
                values(:articleId,:provider,:model,:status,:resultJson,:errorMessage,:now,:now)
                """)
                .param("articleId", articleId).param("provider", provider).param("model", model)
                .param("status", status).param("resultJson", resultJson)
                .param("errorMessage", errorMessage).param("now", now).update();
        return map(articleId, provider, model, status, resultJson, errorMessage, now);
    }

    private ArticleIntelligenceView map(long articleId, String provider, String model, String status,
                                        String resultJson, String errorMessage, OffsetDateTime analyzedAt) {
        Map<String, Object> values = Map.of();
        try {
            if (resultJson != null && !resultJson.isBlank()) {
                values = json.readValue(resultJson, new TypeReference<>() {
                });
            }
        } catch (Exception exception) {
            throw new IllegalStateException("AI 结果读取失败", exception);
        }
        return new ArticleIntelligenceView(articleId, provider, model, status,
                text(values, "oneSentenceSummary"), text(values, "coreSummary"), strings(values, "outline"),
                strings(values, "keyPoints"), strings(values, "keyConclusions"), strings(values, "keyData"),
                strings(values, "importantCases"), strings(values, "people"), strings(values, "organizations"),
                strings(values, "products"), strings(values, "technologies"), strings(values, "locations"),
                strings(values, "timeInformation"), strings(values, "keywords"), strings(values, "tags"),
                text(values, "category"), text(values, "articleType"), integer(values, "readingValue"),
                integer(values, "qualityScore"), text(values, "sourceCredibility"), text(values, "readingReason"),
                strings(values, "informationNature"), integer(values, "promptTokens"),
                integer(values, "responseTokens"), longValue(values, "durationMillis"),
                errorMessage, analyzedAt);
    }

    private String text(Map<String, Object> values, String name) {
        Object value = values.get(name);
        return value == null ? null : value.toString();
    }

    private List<String> strings(Map<String, Object> values, String name) {
        Object value = values.get(name);
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        return list.stream().map(Object::toString).toList();
    }

    private Integer integer(Map<String, Object> values, String name) {
        Object value = values.get(name);
        return value instanceof Number number ? number.intValue() : null;
    }

    private Long longValue(Map<String, Object> values, String name) {
        Object value = values.get(name);
        return value instanceof Number number ? number.longValue() : null;
    }
}
