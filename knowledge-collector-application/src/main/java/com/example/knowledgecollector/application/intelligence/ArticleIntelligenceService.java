package com.example.knowledgecollector.application.intelligence;

import com.example.knowledgecollector.application.article.ArticleService;
import com.example.knowledgecollector.application.exception.BusinessRuleException;
import com.example.knowledgecollector.application.knowledge.KnowledgeWorkspaceService;
import com.example.knowledgecollector.capability.intelligence.ContentIntelligenceProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class ArticleIntelligenceService {
    private final ArticleService articles;
    private final ArticleIntelligenceGateway gateway;
    private final List<ContentIntelligenceProvider> providers;
    private final String defaultProvider;
    private final KnowledgeWorkspaceService knowledge;

    public ArticleIntelligenceService(ArticleService articles, ArticleIntelligenceGateway gateway,
                                      List<ContentIntelligenceProvider> providers,
                                      @Value("${knowledge-collector.ai.provider:ollama}") String defaultProvider,
                                      KnowledgeWorkspaceService knowledge) {
        this.articles = articles;
        this.gateway = gateway;
        this.providers = providers;
        this.defaultProvider = defaultProvider;
        this.knowledge = knowledge;
    }

    public List<ContentIntelligenceProvider.ProviderStatus> providers() {
        return providers.stream().map(ContentIntelligenceProvider::status).toList();
    }

    public ArticleIntelligenceView get(long articleId) {
        articles.get(articleId);
        return gateway.find(articleId).orElse(null);
    }

    public ArticleIntelligenceView analyze(long articleId, String providerId) {
        var article = articles.get(articleId);
        String selectedProvider = providerId == null || providerId.isBlank() ? defaultProvider : providerId;
        String content = article.contentText();
        if (content == null || content.isBlank()) {
            content = article.summary();
        }
        if (content == null || content.isBlank()) {
            throw new BusinessRuleException("AI-CONTENT-EMPTY", "文章没有可供 AI 分析的正文或摘要");
        }
        ContentIntelligenceProvider provider = providers.stream()
                .filter(candidate -> candidate.id().equalsIgnoreCase(selectedProvider))
                .findFirst()
                .orElseThrow(() -> new BusinessRuleException(
                        "AI-PROVIDER-NOT-AVAILABLE", "未找到 AI Provider：" + selectedProvider));
        try {
            var result = provider.analyze(new ContentIntelligenceProvider.AnalysisRequest(
                    article.title(), content, Set.of(
                    ContentIntelligenceProvider.Capability.SUMMARY,
                    ContentIntelligenceProvider.Capability.KEYWORD_EXTRACTION,
                    ContentIntelligenceProvider.Capability.ENTITY_EXTRACTION,
                    ContentIntelligenceProvider.Capability.TAG_GENERATION,
                    ContentIntelligenceProvider.Capability.CLASSIFICATION,
                    ContentIntelligenceProvider.Capability.QUALITY_SCORING)));
            ArticleIntelligenceView saved = gateway.saveSuccess(articleId, result);
            knowledge.saveAiRecommendedCards(articleId, result.values());
            return saved;
        } catch (Exception exception) {
            gateway.saveFailure(articleId, provider.id(), exception.getMessage());
            throw new BusinessRuleException("AI-ANALYSIS-FAILED", exception.getMessage());
        }
    }
}
