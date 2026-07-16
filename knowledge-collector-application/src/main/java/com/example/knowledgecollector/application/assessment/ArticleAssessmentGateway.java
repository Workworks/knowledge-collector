package com.example.knowledgecollector.application.assessment;

import com.example.knowledgecollector.domain.article.ArticleAssessmentRules;

public interface ArticleAssessmentGateway {
    void save(long articleId, ArticleAssessmentRules.Assessment assessment);
    ArticleAssessmentView get(long articleId);
}
