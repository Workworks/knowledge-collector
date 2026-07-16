package com.example.knowledgecollector;

import com.example.knowledgecollector.domain.source.SourceRules;
import com.example.knowledgecollector.domain.topic.TopicRules;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TopicRulesTest {

    @Test
    void normalizesCodesAndKeywordLists() {
        assertThat(TopicRules.normalizeCode(" ai-news ")).isEqualTo("AI-NEWS");
        assertThat(TopicRules.normalizeTerms("人工智能， Java\n人工智能, Spring "))
                .containsExactly("人工智能", "Java", "Spring");
    }

    @Test
    void acceptsOnlyHttpAndHttpsSourceUrls() {
        assertThat(SourceRules.isHttpUrl("https://example.com/feed")).isTrue();
        assertThat(SourceRules.isHttpUrl("http://example.com")).isTrue();
        assertThat(SourceRules.isHttpUrl("file:///etc/passwd")).isFalse();
        assertThat(SourceRules.isHttpUrl("javascript:alert(1)")).isFalse();
    }
}
