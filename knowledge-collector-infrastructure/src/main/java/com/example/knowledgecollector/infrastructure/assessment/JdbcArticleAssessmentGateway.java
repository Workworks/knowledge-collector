package com.example.knowledgecollector.infrastructure.assessment;

import com.example.knowledgecollector.application.assessment.ArticleAssessmentGateway;
import com.example.knowledgecollector.application.assessment.ArticleAssessmentView;
import com.example.knowledgecollector.application.exception.ResourceNotFoundException;
import com.example.knowledgecollector.domain.article.ArticleAssessmentRules;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

@Repository
public class JdbcArticleAssessmentGateway implements ArticleAssessmentGateway {
    private final JdbcClient jdbc;

    public JdbcArticleAssessmentGateway(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional
    public void save(long articleId, ArticleAssessmentRules.Assessment assessment) {
        jdbc.sql("""
                update article set content_fingerprint=:fingerprint,quality_score=:quality,
                review_status=:reviewStatus,source_level=:sourceLevel,evidence_count=:evidence,
                assessment_warnings=:warnings,updated_at=:updatedAt where id=:articleId
                """)
                .param("fingerprint", assessment.fingerprint())
                .param("quality", assessment.qualityScore())
                .param("reviewStatus", assessment.reviewStatus())
                .param("sourceLevel", assessment.sourceLevel())
                .param("evidence", assessment.evidenceCount())
                .param("warnings", String.join("\n", assessment.warnings()))
                .param("updatedAt", OffsetDateTime.now())
                .param("articleId", articleId).update();
        jdbc.sql("delete from article_topic_rel where article_id=:articleId")
                .param("articleId", articleId).update();
        assessment.topicMatches().forEach((topicId, match) ->
                jdbc.sql("""
                        insert into article_topic_rel(article_id,topic_id,match_score,match_reason,created_at)
                        values(:articleId,:topicId,:score,:reason,:createdAt)
                        """)
                        .param("articleId", articleId).param("topicId", topicId)
                        .param("score", match.score()).param("reason", match.reason())
                        .param("createdAt", OffsetDateTime.now()).update());
    }

    @Override
    public ArticleAssessmentView get(long articleId) {
        var rows = jdbc.sql("""
                select id,quality_score,review_status,source_level,evidence_count,
                content_fingerprint,assessment_warnings,
                case when source_level='PRIMARY_SOURCE' then true else false end as has_doi
                from article where id=:articleId
                """).param("articleId", articleId).query().listOfRows();
        if (rows.isEmpty()) {
            throw new ResourceNotFoundException("文章不存在：" + articleId);
        }
        var row = rows.get(0);
        List<ArticleAssessmentView.TopicMatchView> matches = jdbc.sql("""
                select r.topic_id,t.topic_name,r.match_score,r.match_reason
                from article_topic_rel r join topic t on t.id=r.topic_id
                where r.article_id=:articleId order by r.match_score desc,t.sort_order,t.id
                """).param("articleId", articleId).query((resultSet, rowNumber) ->
                new ArticleAssessmentView.TopicMatchView(
                        resultSet.getLong("topic_id"), resultSet.getString("topic_name"),
                        resultSet.getInt("match_score"), resultSet.getString("match_reason"))).list();
        String warnings = (String) row.get("ASSESSMENT_WARNINGS");
        return new ArticleAssessmentView(
                articleId, ((Number) row.get("QUALITY_SCORE")).intValue(),
                (String) row.get("REVIEW_STATUS"), (String) row.get("SOURCE_LEVEL"),
                ((Number) row.get("EVIDENCE_COUNT")).intValue(), Boolean.TRUE.equals(row.get("HAS_DOI")),
                (String) row.get("CONTENT_FINGERPRINT"),
                warnings == null || warnings.isBlank() ? List.of() : Arrays.asList(warnings.split("\\r?\\n")),
                matches);
    }
}
