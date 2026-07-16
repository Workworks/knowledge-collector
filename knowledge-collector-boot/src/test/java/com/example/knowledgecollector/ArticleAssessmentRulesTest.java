package com.example.knowledgecollector;

import com.example.knowledgecollector.domain.article.ArticleAssessmentRules;
import com.example.knowledgecollector.domain.topic.Topic;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ArticleAssessmentRulesTest {
    @Test
    void matchesMultipleTopicsAndCreatesExplainableQualityResult() {
        var science = topic(1L, "科学", List.of("memory", "sleep"), List.of("advertisement"));
        var cognition = topic(2L, "认知", List.of("active recall"), List.of());
        var result = ArticleAssessmentRules.assess(new ArticleAssessmentRules.Input(
                "Active recall improves memory", "Research Team",
                "A detailed scientific summary about memory and learning mechanisms.",
                "Active recall and sleep both contribute to long term memory consolidation. ".repeat(5),
                "https://doi.org/10.1000/example", true
        ), List.of(science, cognition), Set.of());

        assertThat(result.topicMatches()).containsKeys(1L, 2L);
        assertThat(result.qualityScore()).isGreaterThanOrEqualTo(80);
        assertThat(result.reviewStatus()).isEqualTo("AUTO_ACCEPTED");
        assertThat(result.sourceLevel()).isEqualTo("PRIMARY_SOURCE");
        assertThat(result.fingerprint()).hasSize(64);
    }

    @Test
    void exclusionKeywordOverridesDefaultTopicAndSendsArticleToReview() {
        var topic = topic(1L, "新闻", List.of("science"), List.of("advertisement"));
        var result = ArticleAssessmentRules.assess(new ArticleAssessmentRules.Input(
                "Science advertisement", null, null, null,
                "http://example.com/item", false
        ), List.of(topic), Set.of(1L));

        assertThat(result.topicMatches()).isEmpty();
        assertThat(result.reviewStatus()).isEqualTo("PENDING_REVIEW");
        assertThat(result.warnings()).contains("未匹配主题");
    }

    private Topic topic(long id, String name, List<String> keywords, List<String> excluded) {
        OffsetDateTime now = OffsetDateTime.now();
        return new Topic(id, "T" + id, name, null, keywords, excluded,
                "#2563EB", null, "en", true, 0, 0, now, now);
    }
}
