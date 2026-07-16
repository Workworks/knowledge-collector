package com.example.knowledgecollector.application.assessment;

import java.util.List;

public record ArticleAssessmentView(
        long articleId, int qualityScore, String reviewStatus, String sourceLevel,
        int evidenceCount, boolean hasDoi, String contentFingerprint,
        List<String> warnings, List<TopicMatchView> topics
) {
    public record TopicMatchView(long topicId, String topicName, int score, String reason) {
    }
}
