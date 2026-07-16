package com.example.knowledgecollector.application.intelligence;

import com.example.knowledgecollector.capability.intelligence.ContentIntelligenceProvider;

import java.util.Optional;

public interface ArticleIntelligenceGateway {
    Optional<ArticleIntelligenceView> find(long articleId);

    ArticleIntelligenceView saveSuccess(long articleId, ContentIntelligenceProvider.AnalysisResult result);

    ArticleIntelligenceView saveFailure(long articleId, String provider, String message);
}
